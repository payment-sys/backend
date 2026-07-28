package com.v_payment.pay.payment.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

class PaymentTest {

    @DisplayName("Payment 생성 시 paymentKey, approvedAmount, approvedAt, receiptUrl만 null을 허용 한다.")
    @Test
    void paymentTest1() {
        //given
        String orderCode = "test-code";
        Long amount = 1000L;
        PaymentMethod paymentMethod = PaymentMethod.TRANSFER;
        Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());

        //when
        Payment payment = Payment.createPendingPayment(orderCode, amount, paymentMethod, clock);

        //then
        Assertions.assertThat(payment).isNotNull();
        Assertions.assertThat(payment.getProvider()).isEqualTo(Provider.TOSS);
        Assertions.assertThat(payment.getPaymentMethod()).isEqualTo(paymentMethod);
        Assertions.assertThat(payment.getOrderCode()).isEqualTo(orderCode);
        Assertions.assertThat(payment.getRequestedAmount()).isEqualTo(amount);
        Assertions.assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.READY);
        Assertions.assertThat(payment.getRequestedAt()).isEqualTo(LocalDateTime.now(clock));
        Assertions.assertThat(payment.getRecoveryAttemptCount()).isZero();

        Assertions.assertThat(payment.getPaymentKey()).isNull();
        Assertions.assertThat(payment.getApprovedAmount()).isNull();
        Assertions.assertThat(payment.getApprovedAt()).isNull();
        Assertions.assertThat(payment.getReceiptUrl()).isNull();

    }
}