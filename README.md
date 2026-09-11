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
