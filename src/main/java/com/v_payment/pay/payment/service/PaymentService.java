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
import com.v_payment.pay.payment.infra.AbortedResult;
import com.v_payment.pay.payment.infra.DoneResult;
import com.v_payment.pay.payment.infra.ExpiredResult;
import com.v_payment.pay.payment.infra.Result;
import com.v_payment.pay.payment.infra.UnknownResult;
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
                doneResult.receipt().url()
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
