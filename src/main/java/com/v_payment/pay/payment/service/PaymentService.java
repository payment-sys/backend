package com.v_payment.pay.payment.service;

import com.v_payment.pay.global.exception.BusinessException;
import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.req.TossPaymentWebhookReq;
import com.v_payment.pay.payment.controller.dto.res.ApprovalRes;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.Result;
import com.v_payment.pay.payment.infra.SuccessResult;
import com.v_payment.pay.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.v_payment.pay.payment.exception.PaymentException.PAYMENT_NOT_FOUND;
import static com.v_payment.pay.payment.exception.PaymentException.UNKNOWN_ERROR;

@Slf4j(topic = "API_LOGGER")
@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final String PAYMENT_STATUS_CHANGED_EVENT = "PAYMENT_STATUS_CHANGED";

    private final PaymentRepository paymentRepository;

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
        if (approveResult instanceof SuccessResult successResult) {
            return applySuccessResult(successResult);
        }
        if (approveResult instanceof FailedResult failedResult) {
            return applyFailedResult(failedResult);
        }
        throw new BusinessException(UNKNOWN_ERROR);
    }

    @Transactional
    public void syncTossPaymentStatus(TossPaymentWebhookReq webhookReq) {
        if (webhookReq == null || !isPaymentStatusChanged(webhookReq)) return;

        TossPaymentWebhookReq.Data payment = webhookReq.data();
        if (payment == null || payment.orderCode() == null || payment.status() == null) {
            log.warn("invalid toss payment webhook payload. eventType = {}", webhookReq.eventType());
            return;
        }

        try {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(payment.status());
            applyWebhookPaymentStatus(payment, paymentStatus);
        } catch (IllegalArgumentException e) {
            log.warn("unsupported toss payment webhook status. eventType = {}, status = {}",
                    webhookReq.eventType(), payment.status());
        }
    }

    private void applyWebhookPaymentStatus(TossPaymentWebhookReq.Data payment, PaymentStatus paymentStatus) {
        int updatedRows = switch (paymentStatus) {
            case READY, IN_PROGRESS -> 0;
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

        log.info("toss payment webhook applied. orderCode = {}, status = {}, updatedRows = {}",
                payment.orderCode(), paymentStatus, updatedRows);
    }

    private boolean isPaymentStatusChanged(TossPaymentWebhookReq webhookReq) {
        return PAYMENT_STATUS_CHANGED_EVENT.equals(webhookReq.eventType());
    }

    private ApprovalRes applySuccessResult(SuccessResult successResult) {
        int updatedRows = paymentRepository.markDone(
                successResult.orderCode(),
                PaymentStatus.IN_PROGRESS,
                PaymentStatus.DONE,
                successResult.totalAmount(),
                successResult.approvedAt(),
                successResult.receipt().url()
        );
        validatePaymentUpdatedRows(updatedRows);
        return ApprovalRes.from(successResult);
    }

    private ApprovalRes applyFailedResult(FailedResult failedResult) {
        int updatedRows = paymentRepository.markAborted(
                failedResult.orderCode(),
                PaymentStatus.IN_PROGRESS,
                PaymentStatus.ABORTED
        );
        validatePaymentUpdatedRows(updatedRows);
        return ApprovalRes.from(failedResult);
    }

    private void validatePaymentUpdatedRows(int updatedRows) {
        if (updatedRows != 1) throw new BusinessException(PAYMENT_NOT_FOUND);
    }
}
