# Outbox 가상 스레드 메모리 최적화 방향

## 배경

현재 구조는 승인 요청을 받은 뒤 `payment_outbox`에 작업을 저장하고, scheduler가 READY outbox를 polling해서 외부 결제 승인 API를 호출한다.

문제는 외부 API 호출 시간이 약 `1초`이고, `max-virtual-thread-tasks`를 키울수록 JVM 힙에 오래 살아 있는 객체가 늘어난다는 점이다.

예를 들어 `200 RPS`를 거의 실시간으로 소비하려면 다음 정도의 in-flight 작업이 필요하다.

```text
필요 in-flight 수 = RPS * 외부 API latency
200 * 1초 = 200개
```

즉, backlog를 줄이려면 단일 인스턴스 또는 여러 인스턴스 어딘가에는 외부 API 호출 중인 작업이 200개 가까이 살아 있어야 한다.

가상 스레드는 플랫폼 스레드보다 가볍지만, 가상 스레드가 대기하는 동안 잡고 있는 객체는 힙에 남는다. 이 객체들이 Young GC를 여러 번 살아남으면 Old 영역으로 승격되고, Old가 차면 Full GC로 이어질 수 있다.

아래 항목들은 단일 JVM에서 in-flight 작업 하나당 힙 점유량과 Old 승격 가능성을 줄이기 위한 최적화 방향이다.

## 1. 외부 API 호출 구간을 `sendAsync` 기반으로 변경

### 현재 문제

현재 outbox 작업은 가상 스레드 안에서 동기 HTTP 호출을 수행한다.

```text
가상 스레드 생성
 -> PaymentPayload 보유
 -> RestClient 동기 호출
 -> 약 1초 대기
 -> Result 생성
 -> 결과 반영
```

동기 호출이 blocking되는 동안 가상 스레드는 park되지만, 해당 가상 스레드의 continuation, stack chunk, lambda 캡처 객체, `PaymentPayload`, HTTP 요청 관련 객체는 힙에 남는다.

작업 수가 100개, 200개로 늘면 이 객체 묶음도 거의 선형으로 늘어난다.

### 개선 방향

JDK `HttpClient.sendAsync()` 또는 비동기 HTTP client를 사용하면 외부 API 대기 동안 작업당 가상 스레드를 계속 붙잡지 않을 수 있다.

개념적으로는 다음 구조가 된다.

```text
polling
 -> 최소 정보만 담은 command 생성
 -> sendAsync 호출
 -> CompletableFuture callback에서 결과 반영 queue로 전달
```

이렇게 하면 1초 동안 대기하는 주체가 가상 스레드가 아니라 HTTP client의 비동기 I/O 상태와 callback이 된다. 작업당 virtual thread continuation/stack chunk 부담을 줄일 수 있다.

### 기대 효과

| 효과 | 설명 |
| --- | --- |
| 가상 스레드 continuation 감소 | 외부 API 대기 시간 동안 가상 스레드 stack을 덜 붙잡음 |
| in-flight 작업당 객체 감소 가능 | 작업 상태를 작은 command/future 중심으로 줄일 수 있음 |
| Old 승격 가능성 완화 | 1초 동안 살아 있는 객체 묶음이 작아짐 |

### 주의점

비동기 HTTP로 바꾸면 복잡도는 올라간다.

- timeout 처리
- 실패 재시도
- callback 예외 처리
- 결과 반영 backpressure
- 종료 시 in-flight future 정리

따라서 단순히 `sendAsync()`로 바꾸는 것만으로 끝나지 않고, 결과 반영 단계까지 bounded 구조로 같이 설계해야 한다.

## 2. in-flight 객체를 최소 필드 record로 축소

### 현재 문제

현재 scheduler는 `PaymentOutboxTask`와 `PaymentPayload`를 만들고, lambda 안에서 이를 캡처해서 외부 API 호출과 결과 반영까지 들고 간다.

대표 흐름:

```text
PaymentOutboxPublishProjection
 -> PaymentOutboxTask
 -> PaymentPayload
 -> lambda 캡처
 -> 외부 API 호출 대기
 -> Result와 함께 결과 반영
```

객체 하나하나는 작지만, in-flight 작업이 100개 이상이면 누적된다. 특히 1초 이상 살아남는 객체는 Young에서 바로 사라지지 않고 Old로 승격될 가능성이 커진다.

### 개선 방향

in-flight 작업은 필요한 필드만 가진 작은 record 하나로 합칠 수 있다.

예:

```java
public record ApprovalCommand(
        long outboxId,
        String orderId,
        String paymentKey,
        long amount
) {
}
```

이렇게 하면 `PaymentOutboxTask`와 `PaymentPayload`를 분리해서 들고 다니는 것보다 객체 수와 참조 수를 줄일 수 있다.

`PaymentPayload`가 외부 API body로 필요하다면, 외부 API 호출 직전에만 생성하거나 `ApprovalCommand` 자체를 request body로 사용할 수도 있다.

### 기대 효과

| 효과 | 설명 |
| --- | --- |
| 객체 수 감소 | task + payload 조합을 command 하나로 축소 |
| 참조 그래프 단순화 | GC가 추적해야 하는 객체 관계 감소 |
| lambda 캡처 감소 | callback이나 runnable이 들고 있는 상태를 줄임 |

### 주의점

무리하게 모든 DTO를 합치면 계층 경계가 흐려질 수 있다. 다만 outbox in-flight 전용 command는 성능 경로이므로 별도 record로 두는 것이 타당하다.

## 3. 결과 반영은 별도 bounded executor 또는 queue로 분리

### 현재 문제

현재 외부 API 호출을 마친 가상 스레드는 결과 반영 limiter를 얻을 때까지 `Result`, `PaymentPayload`, outbox id를 들고 기다릴 수 있다.

흐름:

```text
외부 API 완료
 -> Result 생성
 -> resultApplyLimiter 대기
 -> DB 결과 반영
```

만약 `max-virtual-thread-tasks`는 크고 `max-result-apply-tasks`는 작으면, 외부 API를 끝낸 작업들이 결과 반영 permit을 기다리면서 메모리에 남는다.

### 개선 방향

외부 API 호출 단계와 DB 결과 반영 단계를 분리한다.

개념적으로는 다음과 같다.

```text
외부 API worker
 -> ApprovalResult 생성
 -> bounded result queue에 offer

result apply worker
 -> queue에서 poll
 -> DB transaction 수행
```

중요한 점은 result queue도 반드시 bounded여야 한다는 것이다. 무한 queue를 쓰면 메모리 문제가 위치만 바뀐다.

### 기대 효과

| 효과 | 설명 |
| --- | --- |
| 단계별 backpressure 명확화 | API 호출 동시성과 DB 반영 동시성을 분리해서 제어 |
| 결과 반영 대기 객체 수 제한 | result queue 크기로 명시적 제한 가능 |
| Hikari pool 보호 | DB 반영 worker 수를 Hikari보다 낮게 유지 |

### 주의점

결과 queue가 가득 찼을 때 정책을 정해야 한다.

- 짧게 기다릴지
- 실패로 보고 outbox를 READY로 되돌릴지
- API 호출 동시성을 줄일지

결제 도메인에서는 결과 유실이 치명적이므로, 메모리 queue만 믿기보다는 DB 상태 전이를 기준으로 복구 가능하게 설계해야 한다.

## 4. 실패 메시지와 예외 문자열을 길게 들고 있지 않기

### 현재 문제

외부 API 실패 시 예외 메시지를 `FailedResult`에 넣고, 이후 outbox `last_error_message`에도 저장한다.

예외 메시지는 생각보다 길 수 있다.

- HTTP response body
- stack trace 일부
- URL, header 정보
- nested exception message

이 문자열이 in-flight 결과 객체에 들어가면 결과 반영 전까지 힙에 남는다. 실패가 많이 발생하는 부하 테스트에서는 문자열 할당과 Old 승격이 커질 수 있다.

### 개선 방향

in-flight 객체에는 짧은 error code와 짧게 자른 message만 보관한다.

예:

```java
private static final int MAX_ERROR_MESSAGE_LENGTH = 300;
```

저장 전 message를 잘라낸다.

```text
NETWORK_TIMEOUT
UPSTREAM_429
UPSTREAM_5XX
UPSTREAM_4XX
UNKNOWN
```

가능하면 상세 원문은 로그에도 샘플링하거나 DEBUG 레벨로 제한한다.

### 기대 효과

| 효과 | 설명 |
| --- | --- |
| 실패 폭주 시 문자열 메모리 감소 | 긴 response/error message가 대량 생존하는 상황 방지 |
| DB 저장량 감소 | outbox last_error_message 크기 제한 |
| GC 부담 감소 | 큰 문자열과 byte array 생존 가능성 감소 |

### 주의점

운영 분석에 필요한 최소 정보는 남겨야 한다. error code, status code, orderId, outboxId 정도는 유지하는 것이 좋다.

## 5. 로그 최소화

### 현재 문제

고RPS 환경에서 INFO 로그는 메모리와 I/O 양쪽에 부담이다.

현재 프로젝트에는 다음 경로가 있다.

- API 요청 시작/종료 로그
- `/payments/approvals` 스레드 체크 로그
- scheduler 로그
- 외부 API 실패 로그

로그 이벤트는 문자열 formatting, MDC, encoder, appender queue 또는 file I/O를 동반한다. 특히 부하 테스트 중 외부 API 실패가 많으면 경고 로그가 대량 생성된다.

### 개선 방향

부하 테스트 중에는 다음을 우선 적용한다.

- `ThreadCheckFilter` 제거 또는 DEBUG 레벨로 변경
- API 요청 시작 로그 제거, 종료 로그만 유지하거나 샘플링
- 외부 API 실패 로그는 error code 중심으로 짧게 남김
- 동일 유형 실패는 rate limit 또는 sampling

예:

```text
outboxId=123 paymentError=UPSTREAM_5XX elapsedMs=1002
```

긴 exception 문자열이나 stack trace는 예상 가능한 실패에서는 남기지 않는다.

### 기대 효과

| 효과 | 설명 |
| --- | --- |
| allocation rate 감소 | 로그 이벤트와 문자열 생성 감소 |
| GC 압박 감소 | 짧은 수명 객체 대량 생성 완화 |
| I/O 경합 감소 | rolling file appender 부담 감소 |

### 주의점

장애 분석에 필요한 로그는 남겨야 한다. 단, 부하 테스트에서 매 요청마다 남기는 로그는 성능 측정값을 왜곡할 수 있다.

## 6. G1 GC로 Full GC spike 완화

### 현재 관측

`jstat` 출력에서 Old 사용량이 점진적으로 증가하다가 Full GC 후 크게 감소하는 패턴이 보였다.

예:

```text
OU 45MiB -> 128MiB 근처
Full GC 발생
OU 128MiB -> 62MiB
```

이는 Old에 실제 장기 생존 객체만 있는 것이 아니라, 외부 API 대기 때문에 Young GC를 여러 번 살아남은 mid-lived 객체가 Old로 승격되었다가 Full GC 때 정리되는 패턴에 가깝다.

### G1을 고려하는 이유

G1은 힙을 region 단위로 관리하고, Old 영역도 concurrent marking 이후 mixed GC로 점진적으로 회수할 수 있다.

기대하는 효과는 다음과 같다.

| 항목 | 기존 세대형 GC에서의 문제 | G1에서 기대하는 점 |
| --- | --- | --- |
| Old 회수 | Old가 차면 큰 Full GC로 이어질 수 있음 | 회수 가치가 높은 region을 나눠서 회수 가능 |
| pause | Full GC pause가 크게 튈 수 있음 | pause를 여러 번의 작은 GC로 분산할 가능성 |
| 튜닝 | Young/Old 고정 구조에 가까움 | pause 목표와 region 기반으로 조정 가능 |

### 예시 JVM 옵션

```bash
-Xms256m
-Xmx256m
-XX:+UseG1GC
-XX:MaxTenuringThreshold=15
-XX:TargetSurvivorRatio=90
-XX:InitiatingHeapOccupancyPercent=30
```

필요하면 Young 영역을 더 키우는 실험도 가능하다.

```bash
-XX:+UnlockExperimentalVMOptions
-XX:G1NewSizePercent=40
-XX:G1MaxNewSizePercent=60
```

### 주의점

G1은 근본 해결책이 아니다. `max-virtual-thread-tasks=100` 또는 `200`으로 인해 live set 자체가 256MiB 힙의 한계를 넘으면, G1로 바꿔도 OOM이나 긴 pause를 완전히 막을 수 없다.

G1의 목적은 다음에 가깝다.

```text
Old로 올라간 mid-lived 객체를 Full GC까지 끌고 가지 않고
더 점진적으로 회수할 가능성을 높이는 것
```

## 우선순위

단일 인스턴스에서 `max-virtual-thread-tasks`를 크게 유지해야 한다면, 적용 우선순위는 다음이 적절하다.

1. 로그 최소화
2. 실패 메시지 길이 제한
3. in-flight command 객체 축소
4. 결과 반영 단계를 bounded queue로 분리
5. 외부 API 호출을 비동기 `sendAsync` 기반으로 변경
6. G1 GC 적용 및 GC 로그 기반 튜닝

## 결론

`max-virtual-thread-tasks`를 줄일 수 없다면, 목표는 작업 수를 줄이는 것이 아니라 작업 하나가 1초 동안 붙잡는 객체량을 줄이는 것이다.

핵심 원칙은 다음과 같다.

```text
1. 외부 API 대기 중인 객체를 작게 만든다.
2. 결과 반영 대기 객체를 bounded queue로 제한한다.
3. 실패/로그 문자열을 짧게 유지한다.
4. GC는 Full GC spike를 완화하는 보조 수단으로 사용한다.
```

이 최적화만으로 단일 256MiB JVM이 200개 in-flight 외부 API 호출을 안정적으로 감당한다고 보장할 수는 없다. 다만 같은 힙에서 버틸 수 있는 in-flight 한계를 높이고, Old 승격과 Full GC로 인한 성능 저하를 줄일 수 있다.
