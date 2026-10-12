package com.v_payment.pay.payment.service;

import com.v_payment.pay.payment.controller.dto.req.TossPaymentWebhookReq;
import com.v_payment.pay.payment.domain.entity.PaymentStatus;
import com.v_payment.pay.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookService {
    private static final String PAYMENT_STATUS_CHANGED_EVENT = "PAYMENT_STATUS_CHANGED";

    private final PaymentRepository paymentRepository;

    @Transactional
    public void syncTossPaymentStatus(TossPaymentWebhookReq webhookReq) {
        if (webhookReq == null || !isPaymentStatusChanged(webhookReq)) return;

        TossPaymentWebhookReq.Data payment = webhookReq.data();
        if (payment == null || payment.orderCode() == null || payment.status() == null) {
            log.warn("Invalid Toss payment webhook request. eventType={}", webhookReq.eventType());
            return;
        }

        PaymentStatus paymentStatus;
        try {
            paymentStatus = PaymentStatus.valueOf(payment.status());
        } catch (IllegalArgumentException e) {
            log.warn("Unsupported Toss payment webhook status. eventType={}, status={}",
                    webhookReq.eventType(), payment.status());
            return;
        }

        applyWebhookPaymentStatus(payment, paymentStatus);
    }

    private void applyWebhookPaymentStatus(TossPaymentWebhookReq.Data payment, PaymentStatus paymentStatus) {
        String idempotencyKey = payment.orderCode();
        int updatedRows = switch (paymentStatus) {
            case READY, UNKNOWN, IN_PROGRESS -> 0;
            case DONE -> paymentRepository.markDone(
                    idempotencyKey,
                    payment.paymentKey(),
                    PaymentStatus.DONE,
                    payment.totalAmount(),
                    payment.approvedAt() == null ? null : payment.approvedAt().toLocalDateTime(),
                    payment.receipt() == null ? null : payment.receipt().url()
            );
            case ABORTED -> paymentRepository.markAborted(
                    idempotencyKey,
                    payment.paymentKey(),
                    PaymentStatus.ABORTED,
                    PaymentStatus.DONE
            );
            case EXPIRED -> paymentRepository.markExpired(
                    idempotencyKey,
                    payment.paymentKey(),
                    PaymentStatus.EXPIRED,
                    PaymentStatus.DONE
            );
        };

        log.info("Toss payment webhook applied. idempotencyKey={}, status={}, updatedRows={}",
                idempotencyKey, paymentStatus, updatedRows);
    }

    private boolean isPaymentStatusChanged(TossPaymentWebhookReq webhookReq) {
        return PAYMENT_STATUS_CHANGED_EVENT.equals(webhookReq.eventType());
    }
}
