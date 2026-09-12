# 주문-결제 서비스
### 주문-결제 시스템의 동시성, 비동기 처리, 장애 복구, 성능 개선을 단계적으로 학습하고 구현한 프로젝트 💵

---

# 주문-결제 시스템 요구사항

### 주문

- 사용자는 `여러 상품을 한 번에 주문`할 수 있다.
- 주문 생성 시 `주문 정보와 주문 상품 정보`를 저장한다.
- 사용자는 `주문 시 주문 여부를 확인`할 수 있고, `주문 결과는 동기/비동기`로 상황에 따라 응답 받을 수 있다.
- `재고가 부족하면 주문을 실패 상태`로 변경한다.

### 결제

- `재고 차감에 성공한 주문만 결제 진행`이 가능하다.
- `사용자는 결제 승인 요청`을 보낼 수 있다.
- `결제 승인 요청 시 외부 PG사(토스)에 결제 승인을 요청`한다.
- `서버는 PG사의 결제 상태와 같아`야 하며 최종적으로 결제 상태는 `EXPIRED`, `DONE`, `ABORTED`가 돼야 한다.
- `중복 승인 요청이나 중복 webhook에도 결제 상태가 일관되게 유지`되어야 한다.
- `미완료 결제는 복구할 수 있어`야 한다.

# 최소 성능 기준(SLO)

### 주문

- 목표 peak 처리량: 200 RPS
- 응답시간 허용 범위: p99 / Max 100ms
- 성공률: 99.99% 이상

### 결제

- 목표 처리량: 200 RPS
- 응답시간 허용 범위: p99 / Max: 1500ms
  - PG 결제 승인 시간 1초를 포함해 결제 응답은 1.5초 이내를 목표로 한다.
- 성공률: 99.99% 이상

*테스트는 주문 요청 2초 후 결제를 진행한다.

# 기술 스택

### 배포/인프라
<img width="60" height="60" alt="image" src="https://github.com/user-attachments/assets/7a4cd445-bef7-479a-8513-ed6e853f280c" />
<img width="60" height="60" alt="image" src="https://github.com/user-attachments/assets/062a77b1-7210-435b-b927-351f9091b4fc" />
<img width="60" height="60" alt="image" src="https://github.com/user-attachments/assets/a4736459-2deb-4602-a65e-4e1476bea32b" />

### APP
<img width="60" height="60" alt="image" src="https://github.com/user-attachments/assets/2fc65333-b74f-4761-a775-ce7219f8bcf1" />
<img width="60" height="60" alt="image" src="https://github.com/user-attachments/assets/71e9a2ba-c5c7-422e-9f13-e1ae9ad0eff0" />
<img width="60" height="60" alt="image" src="https://github.com/user-attachments/assets/b416daac-f69e-4b97-be1b-3d2452f3e5a6" />

### 관측
<img width="60" height="60" alt="image" src="https://github.com/user-attachments/assets/86b3397c-f038-49ef-9c72-faf80786a6c4" />
<img width="60" height="60" alt="image" src="https://github.com/user-attachments/assets/a69a308d-5c64-45aa-815b-4b2644319fcd" />


# ERD

<img width="1421" height="639" alt="image" src="https://github.com/user-attachments/assets/7dcad148-3865-428f-81f9-59613182cbdc" />


# Architecture

<img width="586" height="499" alt="{984A6B48-AF54-48E4-8A6D-54BAE2EFA963}" src="https://github.com/user-attachments/assets/f685d7ae-ca5a-463f-9dcf-c1ac745dae57" />

1. ALB: 여러 Spring Boot 인스턴스로 들어오는 요청을 라운드 로빈 방식으로 분산하기 위해 AWS Application Load Balancer를 사용했습니다.
    - ALB는 여러 AZ에 Active-Active로 구성되어, 특정 AZ 장애 시에도 다른 AZ를 통해 요청을 받을 수 있도록 했습니다.
2. AZ 1, 2, 3: 애플리케이션 인스턴스를 여러 가용 영역에 분산 배치하여, 한 AZ에 장애가 발생해도 전체 서비스가 중단되지 않도록 했습니다.
3. Public Subnet: 비용 절감을 위해 애플리케이션 인스턴스를 Public Subnet에 배치하되, 보안 그룹에서 ALB의 요청만 허용하도록 제한했습니다.
    - 운영 환경에서는 Private Subnet + NAT Gateway 구성이 더 안전하지만, 실험 환경에서는 비용을 고려해 Public Subnet을 선택했습니다.
4. Alloy / Grafana / Loki / Tempo: Grafana Alloy를 통해 서버 지표, 애플리케이션 지표, 로그, 트레이스를 수집하고 Grafana 서버로 전송하도록 구성했습니다.
    - Prometheus: Node, Process, Actuator 지표 수집
    - Loki: 애플리케이션 로그 수집
    - Tempo: OpenTelemetry 기반 Trace 수집
    - Grafana: 지표, 로그, 트레이스 통합 시각화
5. MySQL RDS: MySQL RDS를 Multi-AZ Active-Standby 구조로 구성하여 Primary 장애 시 Standby가 자동 승격되도록 했습니다.
6. Spring Boot: 주문, 재고 차감 이벤트, 결제 대기 생성 등 핵심 비즈니스 로직을 처리하는 애플리케이션 서버입니다.
7. Git / GitHub Actions: GitHub Actions를 통해 빌드, 테스트, 이미지 생성, 배포 과정을 자동화하고, 여러 인스턴스에 순차적으로 배포하는 Rolling 방식을 사용했습니다.

# 핵심 구현 내용과 플로우 차트

### 주문과 재고 차감

- 주문 생성 시 주문 정보와 주문 상품 정보를 저장한다.
- 주문 생성 후 상품 수량 차감 이벤트를 `READY` 상태로 저장한다.
- 주문 API는 재고 차감과 결제를 직접 처리하지 않고 주문 접수 여부를 빠르게 응답한다.
- 상품 수량 차감은 DB 기반 이벤트 큐로 비동기 처리한다.
- 이벤트 소비 시 `FOR UPDATE SKIP LOCKED`로 중복 소비를 방지한다.
- 성공 이벤트는 재고를 차감하고, 실패 이벤트는 주문을 실패 상태로 변경한다.
- 수량 차감은 배치 처리한다.

<img width="617" height="472" alt="image" src="https://github.com/user-attachments/assets/e0b7ecbd-c87a-40be-8ea9-95ee57de2ea9" />

### 결제

- 재고 차감에 성공한 주문만 `READY` 결제 데이터로 생성한다.
- 결제 승인 요청 시 외부 PG사인 토스페이먼츠에 승인 요청을 보낸다.
- PG 응답에 따라 결제 상태를 `DONE`, `ABORTED`, `EXPIRED`, `UNKNOWN`으로 반영한다.

<img width="617" height="342" alt="image" src="https://github.com/user-attachments/assets/769a7804-77fc-4da7-b20f-cc665b625af9" />

### 복구와 일관성

- 이벤트 소비 실패 시 `RETRY` 상태로 변경하고, `nextAttemptTime` 이후 재시도한다.
- 미완료 결제는 webhook과 recovery scheduler로 PG 상태를 재조회해 보정한다.
- 중복 승인 요청이나 중복 webhook에도 결제 상태가 일관되게 유지되도록 처리했다.

# 구현 과정과 문제 해결 과정

|순서|제목|설명|wiki 주소|
|---|----|---|---------|
|1-결제 API|PG 호출과 트랜잭션의 분리|외부 API(PG) 결제 승인 호출 시 1~1.5초의 시간 발생으로 트랜잭션 내부에서 호출 시 HikariCP의 병목 예상이 되었음. <br><br> 트랜잭션과 분리하여 해결|<a href=https://github.com/payment-sys/backend/wiki/v1-%E2%80%90%EA%B5%AC%ED%98%84:-%EB%8F%99%EA%B8%B0%EC%8B%9D-%EA%B2%B0%EC%A0%9C-%EC%8B%9C%EC%8A%A4%ED%85%9C-%EA%B5%AC%ED%98%84> 정리 글 </a>|
|2-결제 API|OSIV, Connection Handler Mode 설정|요청 시작 시 EntityManager(Session) 생성하고, 응답 시 삭제하는 OSIV 설정.<br> 첫 SQL이 나갈 때, 커넥션을 획득하고, EntityManager가 삭제될 때 커넥션을 반납하는 Connection Handling Mode 설정.<br> 두 설정이 겹쳐 외부 API 호출이 트랜잭션 내부에 포함되게 됨. <br><br> OSIV를 끔으로 해결|<a href=https://github.com/payment-sys/backend/wiki/v1%E2%80%90%EB%AC%B8%EC%A0%9C%ED%95%B4%EA%B2%B0:-%EA%B2%B0%EC%A0%9C-%EC%8A%B9%EC%9D%B8-API-%EC%84%B1%EB%8A%A5-%ED%85%8C%EC%8A%A4%ED%8A%B8-%EC%A4%91-HikariCP-%EC%BB%A4%EB%84%A5%EC%85%98-%ED%92%80-%EB%B3%91%EB%AA%A9-%EB%B6%84%EC%84%9D-%EB%B0%8F-OSIV-%EB%B9%84%ED%99%9C%EC%84%B1%ED%99%94%EB%A5%BC-%ED%86%B5%ED%95%9C-%EA%B0%9C%EC%84%A0> 정리 글 </a>|
|3-결제 API|동기식 결제 시스템 한계|`요청->검증->PG 호출->결과 저장->응답`이라는 과정이 동기식으로 묶임.<br> 약 1.1~1.6초간을 하나의 Tomcat Worker를 잡은 채 수행됨.<br> 즉, Tomcat Worker와 비즈니스 로직 수행 쓰레드가 1:1로 매핑됨.<br><br> 대량의 쓰레드가 필요한 동기식 방식의 한계를 인식|<a href=https://github.com/payment-sys/backend/wiki/Tomcat-Worker-%EA%B0%AF%EC%88%98%EC%97%90-%EB%94%B0%EB%A5%B8-%EB%8F%99%EA%B8%B0-%EA%B2%B0%EC%A0%9C-%EC%8B%9C%EC%8A%A4%ED%85%9C%EC%9D%98-%ED%95%9C%EA%B3%84> 정리 글 </a>|
|4-결제 API|가상 쓰레드와 CompletableFuture를 이용한 비동기 모델 도입|요청 시 CompletableFuture를 등록하여, 빠르게 Tomcat Worker를 반환하게 함. 비교적 무거운 DB, 외부 API 작업을 가상 쓰레드로 수행하게 함<br> |<a href=https://github.com/payment-sys/backend/wiki/%EA%B0%80%EC%83%81-%EC%93%B0%EB%A0%88%EB%93%9C-%EB%8F%84%EC%9E%85%EC%9C%BC%EB%A1%9C-%EC%9D%B8%ED%95%9C-CPU-%EB%B6%80%ED%95%98-%EA%B0%9C%EC%84%A0> 정리 글 </a>|
|5-결제 API|가상 쓰레드 도입 시 HttpClient NIO 방식 VS 블로킹 방식|가상 쓰레드 도입 후 CPU PSI(15초)가 약 50~70% 가량 발생.<br> 가상 쓰레드 방식 사용 시 외부 API 호출에서 NIO 방식 사용 시 불필요한 쓰레드 전환 비용이 발생됨을 확인.<br><br> 즉, 블로킹 방식의 Httpclient가 더 효율적임을 인식했지만 여전히 CPU 병목이 확인됨. <br>주문 API 구현 이후 동시에 관찰하고자 함 |<a href=https://github.com/payment-sys/backend/wiki/CPU-%EB%B3%91%EB%AA%A9-%ED%99%95%EC%9D%B8%EA%B3%BC-%EC%8B%A4%ED%97%98-%E2%80%90-HttpClient-NIO%EC%97%90%EC%84%9C-%EA%B0%80%EC%83%81%EC%93%B0%EB%A0%88%EB%93%9C%EB%A5%BC-%EC%9D%B4%EC%9A%A9%ED%95%9C-%EB%8F%99%EA%B8%B0%EC%A0%81-%EB%B0%A9%EC%8B%9D%EC%9C%BC%EB%A1%9C-%EB%B3%80%EA%B2%BD> 정리 글 </a>|
|6-주문 API|락 방식의 재고 차감의 한계. 쓰기 캐시의 경우|DB를 SSoT로 사용하고, 캐시는 차감만 보조하는 Write-through 방식을 사용.<br> 단순히 조회 후 업데이트까지의 시간만 줄이려고자 함<br>서버 다운 시 유실 방지를 위해 로컬 캐시에 쓰기 시 Write-Ahead-Log를 파일로 작성.<br><br> HikariCP 병목이 조금은 완화되었으나, 여전히 병목으로 인한 Error 0.15% 가량 발생|<a href=https://github.com/payment-sys/backend/wiki/%EC%A3%BC%EB%AC%B8-%EC%83%9D%EC%84%B1-%EC%A4%91-hot-row-%EB%9D%BD-%EA%B2%BD%ED%95%A9%EC%9C%BC%EB%A1%9C-%EC%9D%B8%ED%95%9C-%EB%AC%B8%EC%A0%9C-%ED%95%B4%EA%B2%B01-%E2%80%90-%EC%BA%90%EC%8B%B1%EC%9D%84-%ED%86%B5%ED%95%9C-%ED%95%B4%EA%B2%B0-%EC%8B%9C%EB%8F%84> 정리 글 </a>|
|7-주문 API|로컬 Queue를 이용한 비동기 Batch 차감 방식|동시 Row 접근이 문제임을 인식하고, 동시성을 줄이고, 처리량은 높이는 Batch 방식을 고려.<br> FIFO 방식의 로컬 Queue와 스케쥴러를 통한 Batch 도입.<br> 서버 다운 차감 정보 유실 방지를 위해, queue 메시지 Log로 남김.<br><br> HikariCP 병목으로 인한 Error율 0.5%->0%로 개선|<a href=https://github.com/payment-sys/backend/wiki/%EB%A1%9C%EC%BB%AC-%EB%B2%84%ED%8D%BC---%ED%8A%B8%EB%9E%9C%EC%9E%AD%EC%85%98-%EB%B6%84%EB%A6%AC%EB%A5%BC-%ED%86%B5%ED%95%9C-HikariCP-%EB%B3%91%EB%AA%A9-%ED%95%B4%EA%B2%B0> 정리 글 </a>|
|8-주문 API|로컬 Queue를 DB Queue로 변경|로컬 Queue를 사용할 시, 수평 확장에 분리함을 인식.<br> 또한, Queue에 이벤트를 저장할 때마다, Log 또한 남겨야 했음.(유실 방지)<br>차라리, DB를 Queue로 사용하기로 결정.|<a href=https://github.com/payment-sys/backend/wiki/%EC%A3%BC%EB%AC%B8-%EC%9E%AC%EA%B3%A0-%EC%B0%A8%EA%B0%90-%EB%B9%84%EB%8F%99%EA%B8%B0-%EB%B2%84%ED%8D%BC(%ED%81%90)-%EC%A0%81%EC%9A%A9-%EC%84%A4%EA%B3%84> 정리 글 </a>|
|9-주문\결제|주문, 결제 400RPS 테스트에서 CPU 병목과 수평 확장|`주문->2초(재고 차감 batch 수행시간)->결제`의 테스트에서 MAX Latency가 3초 이상 발생하는 것을 확인.<br>CPU PSI(15초)도 40% 가량임을 확인.<br>이러한 이유들로, HikariCP가 늦게 반납되어, 병목 발생.<br> 인스턴스를 코어가 4개인 xLarge로 바꿔 테스트한 결과, 에러 없이 MAX 1.3s로 개선됨을 확인.(즉, CPU 병목 맞았음)<br>다만, xLarge의 비용이 기존보다 8배 이상.<br><br> 수직 확장은 8배 수평확장은 4배 정도의 비용임을 확인. 수평 확장 적용|<a href=https://github.com/payment-sys/backend/wiki/%EC%88%98%ED%8F%89-%ED%99%95%EC%9E%A5%EC%9D%84-%ED%86%B5%ED%95%9C-MAX-Latency-%EC%95%88%EC%A0%95%ED%99%94> 정리 글 </a>
|10-주문\결제|수평 확장 시 Payment 중복 생성과 ShedLock|batch scheduler 기존 1개에서 수평확장으로 3개가 되어, 결제 중복 생성 문제 발생.<br> 락을 통해 해결하고자 하였으나, 순서 보장 불가 및 병목 발생.<br> 기존 단일 스케쥴러에서도 병목 발생하지 않았음을 고려하여, ShedLock 적용.<br><br> error 0%와 기존 SLO 목표치 달성|<a href=https://github.com/payment-sys/backend/wiki/%EC%88%98%ED%8F%89%ED%99%95%EC%9E%A5%EC%9C%BC%EB%A1%9C-%EC%9D%B8%ED%95%9C,-PENDING-Payment-%EC%A4%91%EB%B3%B5-%EC%83%9D%EC%84%B1-%EB%AC%B8%EC%A0%9C-%ED%95%B4%EA%B2%B0-With-ShedLock> 정리 글 </a>|
