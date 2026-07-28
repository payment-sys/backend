package com.v_payment.pay.payment.service;

import com.v_payment.pay.global.exception.BusinessException;
import com.v_payment.pay.order.entity.OrderStatus;
import com.v_payment.pay.order.service.OrderManager;
import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.req.TossPaymentWebhookReq;
import com.v_payment.pay.payment.controller.dto.res.ApprovalRes;
import com.v_payment.pay.payment.config.PaymentRecoveryProperties;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.infra.result.AbortedResult;
import com.v_payment.pay.payment.infra.result.DoneResult;
import com.v_payment.pay.payment.infra.result.ExpiredResult;
import com.v_payment.pay.payment.infra.result.Result;
import com.v_payment.pay.payment.infra.result.UnknownResult;
import com.v_payment.pay.payment.repository.PaymentRepository;
import com.v_payment.pay.product.service.ProductManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static com.v_payment.pay.payment.exception.PaymentException.PAYMENT_NOT_FOUND;
import static com.v_payment.pay.payment.exception.PaymentException.UNKNOWN_ERROR;

/**
 * unit test
 * DisplayName: 결제 승인 요청 검증 시 payment를 IN_PROGRESS로 변경하고 PaymentPayload를 반환한다.
 * Given: ApprovalReq가 준비되어 있고 paymentRepository.markInProgress()가 1을 반환하도록 stub한다.
 * When: validateApprovalReq(approvalReq)를 호출한다.
 * Then: markInProgress()가 요청 값과 READY, IN_PROGRESS 상태로 호출되고, ApprovalReq 값으로 생성된 PaymentPayload를 반환한다.
 *
 * DisplayName: 결제 승인 요청 검증 시 변경된 payment가 없으면 BusinessException을 던진다.
 * Given: ApprovalReq가 준비되어 있고 paymentRepository.markInProgress()가 0을 반환하도록 stub한다.
 * When: validateApprovalReq(approvalReq)를 호출한다.
 * Then: PAYMENT_NOT_FOUND BusinessException을 던진다.
 *
 * finalizePaymentPayload - DoneResult
 * DisplayName: DoneResult 처리 시 payment를 DONE으로 변경하고 주문 상태를 PAID로 변경한다.
 * Given: DoneResult가 준비되어 있고 paymentRepository.markDone()이 1을 반환하도록 stub한다.
 * When: finalizePaymentPayload(doneResult)를 호출한다.
 * Then: markDone()이 IN_PROGRESS, UNKNOWN, DONE 상태와 승인 정보로 호출되고, orderManager.updateStatus(orderCode, PAID)가 호출되며 Done ApprovalRes를 반환한다.
 *
 * DisplayName: DoneResult 처리 시 변경된 payment가 없으면 주문 상태를 변경하지 않고 BusinessException을 던진다.
 * Given: DoneResult가 준비되어 있고 paymentRepository.markDone()이 0을 반환하도록 stub한다.
 * When: finalizePaymentPayload(doneResult)를 호출한다.
 * Then: PAYMENT_NOT_FOUND BusinessException을 던지고 orderManager와 productManager는 호출하지 않는다.
 *
 * finalizePaymentPayload - AbortedResult
 * DisplayName: AbortedResult 처리 시 payment를 ABORTED로 변경하고 주문 상태를 PAYMENT_FAILED로 변경한다.
 * Given: AbortedResult가 준비되어 있고 paymentRepository.markAborted()가 1을 반환하며 orderManager.updateStatus()가 updated true와 주문 상품 목록을 반환하도록 stub한다.
 * When: finalizePaymentPayload(abortedResult)를 호출한다.
 * Then: markAborted()가 IN_PROGRESS, UNKNOWN, ABORTED 상태로 호출되고, orderManager.updateStatus(orderCode, PAYMENT_FAILED)와 productManager.restore()가 호출되며 Aborted ApprovalRes를 반환한다.
 *
 * DisplayName: AbortedResult 처리 시 주문 상태가 실제로 변경되지 않았다면 상품 재고를 복구하지 않는다.
 * Given: AbortedResult가 준비되어 있고 paymentRepository.markAborted()가 1을 반환하며 orderManager.updateStatus()가 updated false를 반환하도록 stub한다.
 * When: finalizePaymentPayload(abortedResult)를 호출한다.
 * Then: orderManager.updateStatus(orderCode, PAYMENT_FAILED)는 호출되지만 productManager.restore()는 호출하지 않는다.
 *
 * finalizePaymentPayload - UnknownResult
 * DisplayName: UnknownResult 처리 시 payment를 UNKNOWN으로 변경하고 주문과 상품은 변경하지 않는다.
 * Given: UnknownResult가 준비되어 있고 paymentRepository.markUnknown()이 1을 반환하도록 stub한다.
 * When: finalizePaymentPayload(unknownResult)를 호출한다.
 * Then: markUnknown()이 IN_PROGRESS, UNKNOWN 상태로 호출되고, orderManager와 productManager는 호출하지 않으며 Unknown ApprovalRes를 반환한다.
 *
 * finalizePaymentPayload - ExpiredResult
 * DisplayName: ExpiredResult 처리 시 payment를 EXPIRED로 변경하고 주문 상태를 CANCELLED로 변경한다.
 * Given: ExpiredResult가 준비되어 있고 paymentRepository.markExpired()가 1을 반환하며 orderManager.updateStatus()가 updated true와 주문 상품 목록을 반환하도록 stub한다.
 * When: finalizePaymentPayload(expiredResult)를 호출한다.
 * Then: markExpired()가 IN_PROGRESS, UNKNOWN, EXPIRED 상태로 호출되고, orderManager.updateStatus(orderCode, CANCELLED)와 productManager.restore()가 호출되며 Expired ApprovalRes를 반환한다.
 *
 * DisplayName: 지원하지 않는 Result 처리 시 UNKNOWN_ERROR BusinessException을 던진다.
 * Given: DoneResult, AbortedResult, UnknownResult, ExpiredResult가 아닌 Result 구현체가 준비되어 있다.
 * When: finalizePaymentPayload(result)를 호출한다.
 * Then: UNKNOWN_ERROR BusinessException을 던지고 paymentRepository, orderManager, productManager는 호출하지 않는다.
 *
 * findRecoveryTargets
 * DisplayName: 복구 대상 조회 시 기준 시간과 batchSize로 복구 가능한 payment를 조회하고 PaymentRecoveryTarget으로 변환한다.
 * Given: Clock이 고정되어 있고 staleAfterSeconds, batchSize가 stub되어 있으며 paymentRepository.findRecoverablePayments()가 payment 목록을 반환하도록 stub한다.
 * When: findRecoveryTargets()를 호출한다.
 * Then: findRecoverablePayments()가 IN_PROGRESS, UNKNOWN 상태와 requestedBefore, PageRequest로 호출되고, payment 값과 recoveryAttemptCount가 PaymentRecoveryTarget에 매핑된다.
 *
 * increaseRecoveryAttemptCount
 * DisplayName: 복구 시도 횟수 증가 시 IN_PROGRESS와 UNKNOWN 상태 payment를 대상으로 repository에 위임한다.
 * Given: orderCode가 준비되어 있다.
 * When: increaseRecoveryAttemptCount(orderCode)를 호출한다.
 * Then: paymentRepository.increaseRecoveryAttemptCount(orderCode, IN_PROGRESS, UNKNOWN)가 호출된다.
 *
 * syncTossPaymentStatus
 * DisplayName: webhookReq가 null이면 아무 작업도 수행하지 않는다.
 * Given: webhookReq가 null이다.
 * When: syncTossPaymentStatus(null)를 호출한다.
 * Then: paymentRepository, orderManager, productManager는 호출하지 않는다.
 *
 * DisplayName: eventType이 PAYMENT_STATUS_CHANGED가 아니면 아무 작업도 수행하지 않는다.
 * Given: eventType이 PAYMENT_STATUS_CHANGED가 아닌 TossPaymentWebhookReq가 준비되어 있다.
 * When: syncTossPaymentStatus(webhookReq)를 호출한다.
 * Then: paymentRepository, orderManager, productManager는 호출하지 않는다.
 *
 * DisplayName: webhook data, orderCode, status 중 하나가 없으면 아무 작업도 수행하지 않는다.
 * Given: data가 null이거나 orderCode 또는 status가 null인 TossPaymentWebhookReq가 준비되어 있다.
 * When: syncTossPaymentStatus(webhookReq)를 호출한다.
 * Then: paymentRepository, orderManager, productManager는 호출하지 않는다.
 *
 * DisplayName: 지원하지 않는 webhook status면 아무 작업도 수행하지 않는다.
 * Given: status가 PaymentStatus enum에 없는 TossPaymentWebhookReq가 준비되어 있다.
 * When: syncTossPaymentStatus(webhookReq)를 호출한다.
 * Then: paymentRepository, orderManager, productManager는 호출하지 않는다.
 *
 * DisplayName: webhook status가 READY, UNKNOWN, IN_PROGRESS이면 payment와 주문 상태를 변경하지 않는다.
 * Given: status가 READY, UNKNOWN, IN_PROGRESS 중 하나인 TossPaymentWebhookReq가 준비되어 있다.
 * When: syncTossPaymentStatus(webhookReq)를 호출한다.
 * Then: paymentRepository, orderManager, productManager는 호출하지 않는다.
 *
 * DisplayName: webhook status가 DONE이고 payment가 변경되면 주문 상태를 PAID로 변경한다.
 * Given: status가 DONE인 TossPaymentWebhookReq가 준비되어 있고 paymentRepository.markDone()이 1을 반환하도록 stub한다.
 * When: syncTossPaymentStatus(webhookReq)를 호출한다.
 * Then: markDone()이 webhook의 orderCode, paymentKey, totalAmount, approvedAt, receiptUrl로 호출되고 orderManager.updateStatus(orderCode, PAID)가 호출된다.
 *
 * DisplayName: webhook status가 DONE이지만 변경된 payment가 없으면 주문 상태를 변경하지 않는다.
 * Given: status가 DONE인 TossPaymentWebhookReq가 준비되어 있고 paymentRepository.markDone()이 0을 반환하도록 stub한다.
 * When: syncTossPaymentStatus(webhookReq)를 호출한다.
 * Then: orderManager와 productManager는 호출하지 않는다.
 *
 * DisplayName: webhook status가 ABORTED이고 payment가 변경되면 주문 상태를 PAYMENT_FAILED로 변경하고 상품 재고를 복구한다.
 * Given: status가 ABORTED인 TossPaymentWebhookReq가 준비되어 있고 paymentRepository.markAborted()가 1을 반환하며 orderManager.updateStatus()가 updated true와 주문 상품 목록을 반환하도록 stub한다.
 * When: syncTossPaymentStatus(webhookReq)를 호출한다.
 * Then: markAborted()가 webhook의 orderCode, paymentKey, ABORTED, DONE 상태로 호출되고, orderManager.updateStatus(orderCode, PAYMENT_FAILED)와 productManager.restore()가 호출된다.
 *
 * DisplayName: webhook status가 EXPIRED이고 payment가 변경되면 주문 상태를 CANCELLED로 변경하고 상품 재고를 복구한다.
 * Given: status가 EXPIRED인 TossPaymentWebhookReq가 준비되어 있고 paymentRepository.markExpired()가 1을 반환하며 orderManager.updateStatus()가 updated true와 주문 상품 목록을 반환하도록 stub한다.
 * When: syncTossPaymentStatus(webhookReq)를 호출한다.
 * Then: markExpired()가 webhook의 orderCode, paymentKey, EXPIRED, DONE 상태로 호출되고, orderManager.updateStatus(orderCode, CANCELLED)와 productManager.restore()가 호출된다.
 */
@Slf4j(topic = "API_LOGGER")
@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final String PAYMENT_STATUS_CHANGED_EVENT = "PAYMENT_STATUS_CHANGED";

    private final Clock clock;
    private final PaymentRepository paymentRepository;
    private final PaymentRecoveryProperties paymentRecoveryProperties;
    private final OrderManager orderManager;
    private final ProductManager productManager;

    @Transactional
    public PaymentPayload validateApprovalReq(ApprovalReq approvalReq) {
        int updatedRows = paymentRepository.markInProgress(
                approvalReq.orderCode(),
                approvalReq.paymentKey(),
                approvalReq.requestedAmount(),
                approvalReq.provider(),
                approvalReq.method(),
                PaymentStatus.READY,
                PaymentStatus.IN_PROGRESS
        );
        validatePaymentUpdatedRows(updatedRows);

        return PaymentPayload.create(approvalReq.orderCode(), approvalReq.paymentKey(), approvalReq.requestedAmount());
    }

    @Transactional
    public ApprovalRes finalizePaymentPayload(Result approveResult) {
        if (approveResult instanceof DoneResult doneResult) {
            return applyDoneResult(doneResult);
        }
        if (approveResult instanceof AbortedResult abortedResult) {
            return applyAbortedResult(abortedResult);
        }
        if (approveResult instanceof UnknownResult unknownResult) {
            return applyUnknownResult(unknownResult);
        }
        if (approveResult instanceof ExpiredResult expiredResult) {
            return applyExpiredResult(expiredResult);
        }
        throw new BusinessException(UNKNOWN_ERROR);
    }

    @Transactional(readOnly = true)
    public List<PaymentRecoveryTarget> findRecoveryTargets() {
        LocalDateTime requestedBefore = LocalDateTime.now(clock)
                .minusSeconds(paymentRecoveryProperties.staleAfterSeconds());
        return paymentRepository.findRecoverablePayments(
                List.of(PaymentStatus.IN_PROGRESS, PaymentStatus.UNKNOWN),
                requestedBefore,
                PageRequest.of(0, paymentRecoveryProperties.batchSize())
        ).stream()
                .map(payment -> new PaymentRecoveryTarget(
                        PaymentPayload.create(
                                payment.getOrderCode(),
                                payment.getPaymentKey(),
                                payment.getRequestedAmount()
                        ),
                        payment.getRecoveryAttemptCount() == null ? 0 : payment.getRecoveryAttemptCount()
                ))
                .toList();
    }

    @Transactional
    public void increaseRecoveryAttemptCount(String orderCode) {
        paymentRepository.increaseRecoveryAttemptCount(
                orderCode,
                PaymentStatus.IN_PROGRESS,
                PaymentStatus.UNKNOWN
        );
    }

    @Transactional
    public void syncTossPaymentStatus(TossPaymentWebhookReq webhookReq) {
        if (webhookReq == null || !isPaymentStatusChanged(webhookReq)) return;

        TossPaymentWebhookReq.Data payment = webhookReq.data();
        if (payment == null || payment.orderCode() == null || payment.status() == null) {
            log.warn("invalid toss payment webhook payload. eventType = {}", webhookReq.eventType());
            return;
        }

        PaymentStatus paymentStatus;
        try {
            paymentStatus = PaymentStatus.valueOf(payment.status());
        } catch (IllegalArgumentException e) {
            log.warn("unsupported toss payment webhook status. eventType = {}, status = {}",
                    webhookReq.eventType(), payment.status());
            return;
        }

        applyWebhookPaymentStatus(payment, paymentStatus);
    }

    private void applyWebhookPaymentStatus(TossPaymentWebhookReq.Data payment, PaymentStatus paymentStatus) {
        int updatedRows = switch (paymentStatus) {
            case READY, UNKNOWN, IN_PROGRESS -> 0;
            case DONE -> paymentRepository.markDone(
                    payment.orderCode(),
                    payment.paymentKey(),
                    PaymentStatus.DONE,
                    payment.totalAmount(),
                    payment.approvedAt() == null ? null : payment.approvedAt().toLocalDateTime(),
                    payment.receipt() == null ? null : payment.receipt().url()
            );
            case ABORTED -> paymentRepository.markAborted(
                    payment.orderCode(),
                    payment.paymentKey(),
                    PaymentStatus.ABORTED,
                    PaymentStatus.DONE
            );
            case EXPIRED -> paymentRepository.markExpired(
                    payment.orderCode(),
                    payment.paymentKey(),
                    PaymentStatus.EXPIRED,
                    PaymentStatus.DONE
            );
        };

        applyOrderPaymentStatus(payment.orderCode(), paymentStatus, updatedRows);

        log.info("toss payment webhook applied. orderCode = {}, status = {}, updatedRows = {}",
                payment.orderCode(), paymentStatus, updatedRows);
    }

    private boolean isPaymentStatusChanged(TossPaymentWebhookReq webhookReq) {
        return PAYMENT_STATUS_CHANGED_EVENT.equals(webhookReq.eventType());
    }

    private ApprovalRes applyDoneResult(DoneResult doneResult) {
        int updatedRows = paymentRepository.markDone(
                doneResult.orderCode(),
                PaymentStatus.IN_PROGRESS,
                PaymentStatus.UNKNOWN,
                PaymentStatus.DONE,
                doneResult.totalAmount(),
                doneResult.approvedAt(),
                doneResult.receipt() == null ? null : doneResult.receipt().url()
        );
        validatePaymentUpdatedRows(updatedRows);
        applyOrderPaid(doneResult.orderCode());
        return ApprovalRes.from(doneResult);
    }

    private ApprovalRes applyAbortedResult(AbortedResult abortedResult) {
        int updatedRows = paymentRepository.markAborted(
                abortedResult.orderCode(),
                PaymentStatus.IN_PROGRESS,
                PaymentStatus.UNKNOWN,
                PaymentStatus.ABORTED
        );
        validatePaymentUpdatedRows(updatedRows);
        applyOrderPaymentFailed(abortedResult.orderCode());
        return ApprovalRes.from(abortedResult);
    }

    private ApprovalRes applyUnknownResult(UnknownResult unknownResult) {
        int updatedRows = paymentRepository.markUnknown(
                unknownResult.orderCode(),
                PaymentStatus.IN_PROGRESS,
                PaymentStatus.UNKNOWN
        );
        validatePaymentUpdatedRows(updatedRows);
        return ApprovalRes.from(unknownResult);
    }

    private ApprovalRes applyExpiredResult(ExpiredResult expiredResult) {
        int updatedRows = paymentRepository.markExpired(
                expiredResult.orderCode(),
                PaymentStatus.IN_PROGRESS,
                PaymentStatus.UNKNOWN,
                PaymentStatus.EXPIRED
        );
        validatePaymentUpdatedRows(updatedRows);
        applyOrderPaymentExpired(expiredResult.orderCode());
        return ApprovalRes.from(expiredResult);
    }

    private void applyOrderPaymentStatus(String orderCode, PaymentStatus paymentStatus, int updatedRows) {
        if (updatedRows != 1) return;

        switch (paymentStatus) {
            case DONE -> applyOrderPaid(orderCode);
            case ABORTED -> applyOrderPaymentFailed(orderCode);
            case EXPIRED -> applyOrderPaymentExpired(orderCode);
            case READY, UNKNOWN, IN_PROGRESS -> {
            }
        }
    }

    private void applyOrderPaid(String orderCode) {
        orderManager.updateStatus(orderCode, OrderStatus.PAID);
    }

    private void applyOrderPaymentFailed(String orderCode) {
        restoreOrderProducts(orderManager.updateStatus(orderCode, OrderStatus.PAYMENT_FAILED));
    }

    private void applyOrderPaymentExpired(String orderCode) {
        restoreOrderProducts(orderManager.updateStatus(orderCode, OrderStatus.CANCELLED));
    }

    private void restoreOrderProducts(OrderManager.OrderStatusUpdateResult updateResult) {
        if (!updateResult.updated()) return;

        productManager.restore(updateResult.orderItems().stream()
                .map(orderItem -> new ProductManager.ProductRestoreReq(orderItem.productId(), orderItem.quantity()))
                .toList());
    }

    private void validatePaymentUpdatedRows(int updatedRows) {
        if (updatedRows != 1) throw new BusinessException(PAYMENT_NOT_FOUND);
    }
}
