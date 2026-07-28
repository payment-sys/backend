# Payment 테스트 시나리오

이 문서는 payment 패키지 테스트 코드 자동화의 사전 정의 시나리오 목록이다.  
`test-code-automation-evaluation.md`의 시나리오 충족률 평가 기준에 따라, 생성된 테스트가 아래 시나리오의 맥락과 핵심 검증을 만족하는지 판단한다.

## 충족 기준

각 시나리오는 아래 조건을 모두 만족할 때 충족한 것으로 본다.

| 조건 | 설명 |
|---|---|
| 시나리오 맥락 일치 | 생성된 테스트가 DisplayName의 동작을 의미상 동일하게 검증한다. |
| 핵심 assertion 포함 | Then에 적힌 반환값, 상태 변화, 예외, 호출 여부를 검증한다. |
| 테스트 실행 성공 | 실제 테스트 실행 환경에서 성공한다. |

## DTO

| ID | 대상 | DisplayName | Given | When | Then |
|---|---|---|---|---|---|
| PAY-DTO-001 | `TossPaymentWebhookReq` | Json의 `orderId` 필드를 `orderCode`로 파싱할 수 있다. | `orderId` 필드를 가진 Json String | `@JsonTest`를 통한 파싱 | `orderCode`에 String 형식의 주문 코드가 존재한다. |

## Controller

| ID | 대상 | DisplayName | Given | When | Then |
|---|---|---|---|---|---|
| PAY-CTRL-001 | `PaymentController.approve` | 결제 승인 요청이 오면 결제 승인 흐름에 위임하고 응답을 반환한다. | 유효한 `ApprovalReq`, `PaymentServiceFacade`는 `CompletableFuture<ApprovalRes>` 응답 stub | `approve()` 호출 | `approvePipeline()`이 호출되고 `CompletableFuture<ApprovalRes>`가 응답된다. |
| PAY-CTRL-002 | `PaymentController.syncTossPaymentStatus` | 결제 상태 변경 웹훅이 오면 결제 상태 동기화 흐름에 위임한다. | 유효한 `TossPaymentWebhookReq`, `PaymentServiceFacade` mock | `syncTossPaymentStatus()` 호출 | `paymentServiceFacade.syncTossPaymentStatus()`가 호출된다. |

## Entity

| ID | 대상 | DisplayName | Given | When | Then |
|---|---|---|---|---|---|
| PAY-ENT-001 | `Payment.createPendingPayment` | 대기 결제 생성 시 초기 상태와 요청 정보를 설정한다. | `orderCode`, `amount`, `paymentMethod`, 고정된 `Clock` | `createPendingPayment()` 호출 | provider, paymentMethod, orderCode, requestedAmount, paymentStatus, requestedAt, recoveryAttemptCount가 설정되고 paymentKey, approvedAmount, approvedAt, receiptUrl은 null이다. |

## Toss Payment

| ID | 대상 | DisplayName | Given | When | Then |
|---|---|---|---|---|---|
| PAY-TOSS-001 | `TossPayment.approve` | 토스 승인 성공 응답을 결제 승인 성공 결과로 변환한다. | `tossPaymentClient`가 `TossPaymentConfirmSuccessRes`를 반환하도록 stub | `approve()` 호출 | 승인 성공 결과 변환 흐름이 수행된다. |
| PAY-TOSS-002 | `TossPayment.approve` | 토스 에러 응답을 결제 실패 결과로 변환한다. | `tossPaymentClient`가 `TossPaymentConfirmErrorRes`를 반환하도록 stub | `approve()` 호출 | 결제 실패 결과 변환 흐름이 수행된다. |
| PAY-TOSS-003 | `TossPayment.approve` | 토스 요청 또는 응답 타임아웃은 확인 불가 결과로 변환한다. | `tossPaymentClient`가 `ResourceAccessException`을 던지도록 stub | `approve()` 호출 | 타임아웃 결과 변환 흐름이 수행된다. |
| PAY-TOSS-004 | `TossPayment.approve` | 토스 승인 중 알 수 없는 예외가 발생하면 확인 불가 결과로 변환한다. | `tossPaymentClient`가 `RuntimeException`을 던지도록 stub | `approve()` 호출 | 알 수 없음 결과 변환 흐름이 수행된다. |
| PAY-TOSS-005 | `TossPayment.approve` | 토스 승인 요청 URI는 설정값을 사용한다. | `tossPaymentProperties.uri()`가 특정 URI를 반환하도록 stub | `approve()` 호출 | 토스 승인 요청 URI가 설정값과 일치한다. |
| PAY-TOSS-006 | `TossPayment.approve` | 토스 승인 요청 헤더에 인증, 콘텐츠 타입, 멱등키가 포함된다. | `secret`, `contentType`, `orderCode` 준비 | `approve()` 호출 | `Authorization`, `Content-Type`, `Idempotency-Key` 헤더가 포함된다. |
| PAY-TOSS-007 | `TossPayment.approve` | 토스 인가 헤더는 secret 값을 Basic Base64 형식으로 인코딩한다. | `tossPaymentProperties.secret()`이 `test_secret`을 반환한다. | `approve(paymentPayload)` 호출 | `Authorization` 헤더 값은 `Basic ` + Base64(`test_secret:`) 이다. |
| PAY-TOSS-008 | `TossPayment.approve` | 토스 승인 요청 바디에는 승인 요청 정보가 전달된다. | `paymentPayload` 준비 | `approve(paymentPayload)` 호출 | 토스 승인 요청 body에 `paymentPayload`가 전달된다. |

## Toss Payment Status Translator

| ID | 대상 | DisplayName | Given | When | Then |
|---|---|---|---|---|---|
| PAY-TR-001 | `TossPaymentStatusTranslator.translate` | 토스가 429를 응답하면 결제 결과를 확인 불가로 분류한다. | `httpStatusCode`가 429인 `TossPaymentConfirmErrorRes`와 `fallbackOrderCode` 준비 | `translate(errorRes, fallbackOrderCode)` 호출 | `UnknownResult`를 반환하고 `paymentError`는 `UPSTREAM_429`이며 `orderCode`와 message가 유지된다. |
| PAY-TR-002 | `TossPaymentStatusTranslator.translate` | 토스가 5xx를 응답하면 결제 결과를 확인 불가로 분류한다. | `httpStatusCode`가 500 이상인 `TossPaymentConfirmErrorRes`와 `fallbackOrderCode` 준비 | `translate(errorRes, fallbackOrderCode)` 호출 | `UnknownResult`를 반환하고 `paymentError`는 `UPSTREAM_5XX`이며 `orderCode`와 message가 유지된다. |
| PAY-TR-003 | `TossPaymentStatusTranslator.translate` | 토스가 결제 세션 없음 코드를 응답하면 결제를 만료로 분류한다. | code가 `NOT_FOUND_PAYMENT_SESSION`인 `TossPaymentConfirmErrorRes`와 `fallbackOrderCode` 준비 | `translate(errorRes, fallbackOrderCode)` 호출 | `ExpiredResult`를 반환하고 `paymentError`는 `UPSTREAM_4XX`이며 `orderCode`와 message가 유지된다. |
| PAY-TR-004 | `TossPaymentStatusTranslator.translate` | 토스가 복구 불가능한 4xx를 응답하면 결제를 실패로 분류한다. | `httpStatusCode`가 500 미만이고 429가 아니며 code가 `NOT_FOUND_PAYMENT_SESSION`이 아닌 `TossPaymentConfirmErrorRes`와 `fallbackOrderCode` 준비 | `translate(errorRes, fallbackOrderCode)` 호출 | `AbortedResult`를 반환하고 `paymentError`는 `UPSTREAM_4XX`이며 `orderCode`와 message가 유지된다. |
| PAY-TR-005 | `TossPaymentStatusTranslator.translate` | 토스 승인 성공 상태가 DONE이면 결제 승인 성공 결과로 분류한다. | status가 `DONE`인 `TossPaymentConfirmSuccessRes`와 `fallbackOrderCode` 준비 | `translate(successRes, fallbackOrderCode)` 호출 | `DoneResult`를 반환하고 orderCode, paymentKey, totalAmount, approvedAt, receipt가 성공 응답 값과 일치한다. |
| PAY-TR-006 | `TossPaymentStatusTranslator.translate` | 토스 승인 성공 응답에 결제 완료 상태가 없으면 확인 불가로 분류한다. | status가 null인 `TossPaymentConfirmSuccessRes`와 `fallbackOrderCode` 준비 | `translate(successRes, fallbackOrderCode)` 호출 | `UnknownResult`를 반환하고 `paymentError`는 `UNKNOWN`이며 message는 `Toss payment status is null`이다. |
| PAY-TR-007 | `TossPaymentStatusTranslator.translateTimeout` | 토스 응답이 타임아웃이면 네트워크 타임아웃 결과를 반환한다. | `fallbackOrderCode`와 timeout message 준비 | `translateTimeout(fallbackOrderCode, message)` 호출 | `UnknownResult`를 반환하고 `paymentError`는 `NETWORK_TIMEOUT`이며 `orderCode`와 message가 유지된다. |
| PAY-TR-008 | `TossPaymentStatusTranslator.translateUnknown` | 토스 승인 중 알 수 없는 예외가 발생하면 확인 불가 결과를 반환한다. | `fallbackOrderCode`와 exception message 준비 | `translateUnknown(fallbackOrderCode, message)` 호출 | `UnknownResult`를 반환하고 `paymentError`는 `UNKNOWN`이며 `orderCode`와 message가 유지된다. |

## Service

| ID | 대상 | DisplayName | Given | When | Then |
|---|---|---|---|---|---|
| PAY-SVC-001 | `PaymentManager.createPendingPayment` | 대기 결제를 생성해 저장한다. | `paymentRepository` mock | `createPendingPayment()` 호출 | `paymentRepository.save()`가 호출된다. |
| PAY-SVC-002 | `PaymentService.validateApprovalReq` | 결제 승인 요청을 시작하면 결제를 진행 중 상태로 바꾸고 승인 요청 정보를 반환한다. | `ApprovalReq`가 준비되어 있고 `paymentRepository.markInProgress()`가 1을 반환하도록 stub | `validateApprovalReq(approvalReq)` 호출 | `markInProgress()`가 요청 값과 READY, IN_PROGRESS 상태로 호출되고 `PaymentPayload`를 반환한다. |
| PAY-SVC-003 | `PaymentService.validateApprovalReq` | 승인할 수 있는 결제가 없으면 예외를 던진다. | `ApprovalReq`가 준비되어 있고 `paymentRepository.markInProgress()`가 0을 반환하도록 stub | `validateApprovalReq(approvalReq)` 호출 | `PAYMENT_NOT_FOUND` `BusinessException`을 던진다. |
| PAY-SVC-004 | `PaymentService.finalizePaymentPayload` | 결제가 승인되면 결제를 완료 처리하고 주문을 결제 완료 상태로 바꾼다. | `DoneResult`가 준비되어 있고 `paymentRepository.markDone()`이 1을 반환하도록 stub | `finalizePaymentPayload(doneResult)` 호출 | `markDone()`이 허용 상태와 승인 정보로 호출되고 `orderManager.updateStatus(orderCode, PAID)`가 호출되며 Done `ApprovalRes`를 반환한다. |
| PAY-SVC-005 | `PaymentService.finalizePaymentPayload` | 결제 완료 처리 대상이 없으면 주문과 상품을 변경하지 않고 예외를 던진다. | `DoneResult`가 준비되어 있고 `paymentRepository.markDone()`이 0을 반환하도록 stub | `finalizePaymentPayload(doneResult)` 호출 | `PAYMENT_NOT_FOUND` `BusinessException`을 던지고 orderManager와 productManager는 호출하지 않는다. |
| PAY-SVC-006 | `PaymentService.finalizePaymentPayload` | 결제가 실패하면 결제를 중단 처리하고 주문을 결제 실패 상태로 바꾼다. | `AbortedResult`가 준비되어 있고 `paymentRepository.markAborted()`가 1을 반환하며 `orderManager.updateStatus()`가 updated true와 주문 상품 목록을 반환하도록 stub | `finalizePaymentPayload(abortedResult)` 호출 | `markAborted()`가 허용 상태로 호출되고 `orderManager.updateStatus(orderCode, PAYMENT_FAILED)`와 `productManager.restore()`가 호출되며 Aborted `ApprovalRes`를 반환한다. |
| PAY-SVC-007 | `PaymentService.finalizePaymentPayload` | 이미 실패 처리된 주문이면 상품 재고를 중복 복구하지 않는다. | `AbortedResult`가 준비되어 있고 `paymentRepository.markAborted()`가 1을 반환하며 `orderManager.updateStatus()`가 updated false를 반환하도록 stub | `finalizePaymentPayload(abortedResult)` 호출 | `orderManager.updateStatus(orderCode, PAYMENT_FAILED)`는 호출되지만 `productManager.restore()`는 호출하지 않는다. |
| PAY-SVC-008 | `PaymentService.finalizePaymentPayload` | 결제 결과를 확인할 수 없으면 결제만 알 수 없음 상태로 남긴다. | `UnknownResult`가 준비되어 있고 `paymentRepository.markUnknown()`이 1을 반환하도록 stub | `finalizePaymentPayload(unknownResult)` 호출 | `markUnknown()`이 허용 상태로 호출되고 orderManager와 productManager는 호출하지 않으며 Unknown `ApprovalRes`를 반환한다. |
| PAY-SVC-009 | `PaymentService.finalizePaymentPayload` | 결제 승인 시간이 만료되면 결제를 만료 처리하고 주문을 취소한다. | `ExpiredResult`가 준비되어 있고 `paymentRepository.markExpired()`가 1을 반환하며 `orderManager.updateStatus()`가 updated true와 주문 상품 목록을 반환하도록 stub | `finalizePaymentPayload(expiredResult)` 호출 | `markExpired()`가 허용 상태로 호출되고 `orderManager.updateStatus(orderCode, CANCELLED)`와 `productManager.restore()`가 호출되며 Expired `ApprovalRes`를 반환한다. |
| PAY-SVC-010 | `PaymentService.finalizePaymentPayload` | 알 수 없는 결제 결과는 처리하지 않고 예외를 던진다. | DoneResult, AbortedResult, UnknownResult, ExpiredResult가 아닌 Result 구현체 준비 | `finalizePaymentPayload(result)` 호출 | `UNKNOWN_ERROR` `BusinessException`을 던지고 paymentRepository, orderManager, productManager는 호출하지 않는다. |
| PAY-SVC-011 | `PaymentService.findRecoveryTargets` | 오래 처리되지 않은 결제를 복구 대상으로 조회한다. | Clock 고정, staleAfterSeconds, batchSize stub, `paymentRepository.findRecoverablePayments()`가 payment 목록 반환 | `findRecoveryTargets()` 호출 | `findRecoverablePayments()`가 IN_PROGRESS, UNKNOWN 상태와 requestedBefore, PageRequest로 호출되고 payment 값과 recoveryAttemptCount가 `PaymentRecoveryTarget`에 매핑된다. |
| PAY-SVC-012 | `PaymentService.increaseRecoveryAttemptCount` | 결제 복구를 시도하면 복구 시도 횟수를 증가시킨다. | `orderCode` 준비 | `increaseRecoveryAttemptCount(orderCode)` 호출 | `paymentRepository.increaseRecoveryAttemptCount(orderCode, IN_PROGRESS, UNKNOWN)`가 호출된다. |
| PAY-SVC-013 | `PaymentService.syncTossPaymentStatus` | 웹훅 요청이 없으면 아무 작업도 수행하지 않는다. | `webhookReq`가 null | `syncTossPaymentStatus(null)` 호출 | paymentRepository, orderManager, productManager는 호출하지 않는다. |
| PAY-SVC-014 | `PaymentService.syncTossPaymentStatus` | 결제 상태 변경 웹훅이 아니면 아무 작업도 수행하지 않는다. | eventType이 PAYMENT_STATUS_CHANGED가 아닌 `TossPaymentWebhookReq` 준비 | `syncTossPaymentStatus(webhookReq)` 호출 | paymentRepository, orderManager, productManager는 호출하지 않는다. |
| PAY-SVC-015 | `PaymentService.syncTossPaymentStatus` | 웹훅에 필수 결제 정보가 없으면 아무 작업도 수행하지 않는다. | data가 null이거나 orderCode 또는 status가 null인 `TossPaymentWebhookReq` 준비 | `syncTossPaymentStatus(webhookReq)` 호출 | paymentRepository, orderManager, productManager는 호출하지 않는다. |
| PAY-SVC-016 | `PaymentService.syncTossPaymentStatus` | 지원하지 않는 웹훅 상태면 아무 작업도 수행하지 않는다. | status가 PaymentStatus enum에 없는 `TossPaymentWebhookReq` 준비 | `syncTossPaymentStatus(webhookReq)` 호출 | paymentRepository, orderManager, productManager는 호출하지 않는다. |
| PAY-SVC-017 | `PaymentService.syncTossPaymentStatus` | 최종 상태가 아닌 웹훅은 결제와 주문을 변경하지 않는다. | status가 READY, UNKNOWN, IN_PROGRESS 중 하나인 `TossPaymentWebhookReq` 준비 | `syncTossPaymentStatus(webhookReq)` 호출 | paymentRepository, orderManager, productManager는 호출하지 않는다. |
| PAY-SVC-018 | `PaymentService.syncTossPaymentStatus` | 결제 완료 웹훅을 받으면 주문을 결제 완료 상태로 바꾼다. | status가 DONE인 `TossPaymentWebhookReq`가 준비되어 있고 `paymentRepository.markDone()`이 1을 반환하도록 stub | `syncTossPaymentStatus(webhookReq)` 호출 | `markDone()`이 웹훅 값으로 호출되고 `orderManager.updateStatus(orderCode, PAID)`가 호출된다. |
| PAY-SVC-019 | `PaymentService.syncTossPaymentStatus` | 이미 반영된 결제 완료 웹훅은 주문 상태를 다시 변경하지 않는다. | status가 DONE인 `TossPaymentWebhookReq`가 준비되어 있고 `paymentRepository.markDone()`이 0을 반환하도록 stub | `syncTossPaymentStatus(webhookReq)` 호출 | orderManager와 productManager는 호출하지 않는다. |
| PAY-SVC-020 | `PaymentService.syncTossPaymentStatus` | 결제 중단 웹훅을 받으면 주문을 결제 실패 처리하고 상품 재고를 복구한다. | status가 ABORTED인 `TossPaymentWebhookReq`가 준비되어 있고 `paymentRepository.markAborted()`가 1을 반환하며 `orderManager.updateStatus()`가 updated true와 주문 상품 목록을 반환하도록 stub | `syncTossPaymentStatus(webhookReq)` 호출 | `markAborted()`가 웹훅 값으로 호출되고 `orderManager.updateStatus(orderCode, PAYMENT_FAILED)`와 `productManager.restore()`가 호출된다. |
| PAY-SVC-021 | `PaymentService.syncTossPaymentStatus` | 결제 만료 웹훅을 받으면 주문을 취소하고 상품 재고를 복구한다. | status가 EXPIRED인 `TossPaymentWebhookReq`가 준비되어 있고 `paymentRepository.markExpired()`가 1을 반환하며 `orderManager.updateStatus()`가 updated true와 주문 상품 목록을 반환하도록 stub | `syncTossPaymentStatus(webhookReq)` 호출 | `markExpired()`가 웹훅 값으로 호출되고 `orderManager.updateStatus(orderCode, CANCELLED)`와 `productManager.restore()`가 호출된다. |

## Facade

| ID | 대상 | DisplayName | Given | When | Then |
|---|---|---|---|---|---|
| PAY-FAC-001 | `PaymentServiceFacade.approvePipeline` | 결제 승인 요청을 받으면 결제 검증, PG 승인, 결과 반영을 순서대로 수행한다. | `ApprovalReq` 준비, 결제 검증 결과 `PaymentPayload`, PG 승인 결과 `Result`, 최종 `ApprovalRes`가 순서대로 반환되도록 stub | `approvePipeline(approvalReq)` 호출 | `validateApprovalReq()`, `tossPayment.approve()`, `finalizePaymentPayload()`가 순서대로 호출되고 `ApprovalRes`를 반환한다. |
| PAY-FAC-002 | `PaymentServiceFacade.approvePipeline` | 결제 검증에 실패하면 PG 승인을 요청하지 않고 예외를 반환한다. | `paymentService.validateApprovalReq()`가 예외를 던지도록 stub | `approvePipeline(approvalReq)` 호출 | `tossPayment.approve()`와 `paymentService.finalizePaymentPayload()`는 호출하지 않고 `CompletableFuture`가 예외로 완료된다. |
| PAY-FAC-003 | `PaymentServiceFacade.approvePipeline` | PG 승인 요청에 실패하면 승인 결과 반영을 수행하지 않고 예외를 반환한다. | `paymentService.validateApprovalReq()`는 `PaymentPayload`를 반환하고 `tossPayment.approve()`가 예외를 던지도록 stub | `approvePipeline(approvalReq)` 호출 | `paymentService.finalizePaymentPayload()`는 호출하지 않고 `CompletableFuture`가 예외로 완료된다. |
| PAY-FAC-004 | `PaymentServiceFacade.approvePipeline` | 결제 결과 반영에 실패하면 예외를 반환한다. | `paymentService.validateApprovalReq()`는 `PaymentPayload`를 반환하고 `tossPayment.approve()`는 `Result`를 반환하며 `paymentService.finalizePaymentPayload()`가 예외를 던지도록 stub | `approvePipeline(approvalReq)` 호출 | `CompletableFuture`가 예외로 완료된다. |
| PAY-FAC-005 | `PaymentServiceFacade.syncTossPaymentStatus` | 결제 상태 변경 웹훅을 받으면 결제 서비스에 처리를 위임한다. | `TossPaymentWebhookReq` 준비 | `syncTossPaymentStatus(webhookReq)` 호출 | `paymentService.syncTossPaymentStatus(webhookReq)`가 호출된다. |

## Recovery Scheduler

| ID | 대상 | DisplayName | Given | When | Then |
|---|---|---|---|---|---|
| PAY-REC-001 | `PaymentRecoveryScheduler.recoverPayments` | 복구 대상 결제가 있으면 각 결제의 승인 상태를 다시 확인하고 결과를 반영한다. | 복구 대상 `PaymentRecoveryTarget` 목록이 준비되어 있고 PG 승인 결과와 결제 결과 반영이 성공하도록 stub | `recoverPayments()` 호출 | 각 복구 대상의 시도 횟수를 증가시키고 PG 승인 요청 후 결제 결과를 반영한다. |
| PAY-REC-002 | `PaymentRecoveryScheduler.recoverPayments` | 복구 대상 결제가 없으면 복구 작업을 수행하지 않는다. | `paymentService.findRecoveryTargets()`가 빈 목록을 반환하도록 stub | `recoverPayments()` 호출 | PG 승인 요청, 복구 시도 횟수 증가, 결제 결과 반영을 수행하지 않는다. |
| PAY-REC-003 | `PaymentRecoveryScheduler.recoverPayments` | 복구 시도가 두 번째 이상이면 에러 로그를 남기고 복구를 계속 진행한다. | `recoveryAttemptCount`가 1 이상인 복구 대상 준비 | `recoverPayments()` 호출 | 재시도 경고 로그를 남기고 복구 시도 횟수 증가, PG 승인 요청, 결제 결과 반영을 수행한다. |
| PAY-REC-004 | `PaymentRecoveryScheduler.recoverPayments` | 특정 결제 복구에 실패해도 다음 복구 대상 처리를 계속한다. | 여러 복구 대상이 준비되어 있고 첫 번째 복구 대상 처리 중 `RuntimeException`이 발생하도록 stub | `recoverPayments()` 호출 | 실패 로그를 남기고 다음 복구 대상의 복구를 계속 수행한다. |
| PAY-REC-005 | `PaymentRecoveryScheduler.recoverPayments` | PG 승인 재시도 결과가 확인 불가이면 결제를 UNKNOWN으로 반영한다. | 복구 대상이 준비되어 있고 `tossPayment.approve()`가 `UnknownResult`를 반환하도록 stub | `recoverPayments()` 호출 | `paymentService.finalizePaymentPayload()`가 `UnknownResult`로 호출된다. |
| PAY-REC-006 | `PaymentRecoveryScheduler.recoverPayments` | PG 승인 재시도 결과가 완료이면 결제와 주문을 완료 상태로 반영한다. | 복구 대상이 준비되어 있고 `tossPayment.approve()`가 `DoneResult`를 반환하도록 stub | `recoverPayments()` 호출 | `paymentService.finalizePaymentPayload()`가 `DoneResult`로 호출된다. |

