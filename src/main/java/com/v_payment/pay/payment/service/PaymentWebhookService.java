package com.v_payment.pay.payment.service;

import com.v_payment.pay.order.service.OrderManager;
import com.v_payment.pay.payment.controller.dto.req.TossPaymentWebhookReq;
import com.v_payment.pay.payment.domain.entity.Payment;
import com.v_payment.pay.payment.domain.entity.PaymentStatus;
import com.v_payment.pay.payment.repository.PaymentRepository;
import com.v_payment.pay.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.v_payment.pay.payment.exception.PaymentException.PAYMENT_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookService {
    private static final String PAYMENT_STATUS_CHANGED_EVENT = "PAYMENT_STATUS_CHANGED";

    private final PaymentRepository paymentRepository;
    private final OrderManager orderManager;

    @Transactional
    public void syncTossPaymentStatus(TossPaymentWebhookReq webhookReq) {
        if (webhookReq == null || !isPaymentStatusChanged(webhookReq)) return;

        TossPaymentWebhookReq.Data payment = webhookReq.data();
        if (payment == null || payment.orderCode() == null || payment.status() == null) {
            log.warn("유효하지 않은 토스 결제 웹훅 요청입니다. eventType={}", webhookReq.eventType());
            return;
        }

        PaymentStatus paymentStatus;
        try {
            paymentStatus = PaymentStatus.valueOf(payment.status());
        } catch (IllegalArgumentException e) {
            log.warn("지원하지 않는 토스 결제 웹훅 상태입니다. eventType={}, status={}",
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

        applyOrderPaymentStatus(idempotencyKey, paymentStatus, updatedRows);

        log.info("토스 결제 웹훅을 반영했습니다. idempotencyKey={}, status={}, updatedRows={}",
                idempotencyKey, paymentStatus, updatedRows);
    }

    private boolean isPaymentStatusChanged(TossPaymentWebhookReq webhookReq) {
        return PAYMENT_STATUS_CHANGED_EVENT.equals(webhookReq.eventType());
    }

    private void applyOrderPaymentStatus(String idempotencyKey, PaymentStatus paymentStatus, int updatedRows) {
        if (updatedRows != 1) return;

        String orderCode = findPayment(idempotencyKey).getOrderCode();
        switch (paymentStatus) {
            case DONE -> applyOrderPaymentDone(orderCode);
            case ABORTED -> applyOrderPaymentFailed(orderCode);
            case EXPIRED -> applyOrderPaymentExpired(orderCode);
            case READY, UNKNOWN, IN_PROGRESS -> {
            }
        }
    }

    private void applyOrderPaymentDone(String orderCode) {
        boolean updated = orderManager.markPaid(orderCode);
        if (!updated) log.error("결제 완료 후 주문 결제완료 상태 업데이트를 실패했습니다. orderCode={}", orderCode);
    }

    private void applyOrderPaymentFailed(String orderCode) {
        boolean updated = orderManager.markLackQuantity(orderCode);
        if (!updated) log.error("결제 실패 후 주문 재고부족 상태 업데이트를 실패했습니다. orderCode={}", orderCode);
    }

    private void applyOrderPaymentExpired(String orderCode) {
        boolean updated = orderManager.markExpired(orderCode);
        if (!updated) log.error("결제 만료 후 주문 만료 상태 업데이트를 실패했습니다. orderCode={}", orderCode);
    }

    private Payment findPayment(String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));
    }
}
