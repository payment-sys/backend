package com.v_payment.pay.payment.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-18T00:00:00Z"),
            ZoneId.of("UTC")
    );

    @DisplayName("Payment는 주문에 대한 결제 시도를 READY 상태로 생성한다.")
    @Test
    void createReady() {
        Payment payment = Payment.createReady("order-code", "idempotency-key", 1000L, PaymentMethod.CARD, CLOCK);

        assertThat(payment.getProvider()).isEqualTo(Provider.TOSS);
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(payment.getOrderCode()).isEqualTo("order-code");
        assertThat(payment.getIdempotencyKey()).isEqualTo("idempotency-key");
        assertThat(payment.getRequestedAmount()).isEqualTo(1000L);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(payment.getRequestedAt()).isEqualTo(LocalDateTime.of(2026, 9, 18, 0, 0));
        assertThat(payment.getRecoveryAttemptCount()).isZero();
    }

    @DisplayName("Payment는 결제 시도를 진행 중으로 바꾼다.")
    @Test
    void markInProgress() {
        Payment payment = Payment.createReady("order-code", "idempotency-key", 1000L, PaymentMethod.CARD, CLOCK);

        payment.markInProgress("payment-key");

        assertThat(payment.getPaymentKey()).isEqualTo("payment-key");
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.IN_PROGRESS);
    }

    @DisplayName("Payment는 결제 시도의 성공 결과를 기록한다.")
    @Test
    void markDone() {
        Payment payment = Payment.createReady("order-code", "idempotency-key", 1000L, PaymentMethod.CARD, CLOCK);
        LocalDateTime approvedAt = LocalDateTime.of(2026, 9, 18, 1, 0);

        payment.markDone(1000L, approvedAt, "receipt-url");

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.DONE);
        assertThat(payment.getApprovedAmount()).isEqualTo(1000L);
        assertThat(payment.getApprovedAt()).isEqualTo(approvedAt);
        assertThat(payment.getReceiptUrl()).isEqualTo("receipt-url");
    }
}
