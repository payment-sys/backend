package com.v_payment.pay.payment.domain;

import com.v_payment.pay.payment.domain.entity.Payment;
import com.v_payment.pay.payment.domain.entity.PaymentMethod;
import com.v_payment.pay.payment.domain.entity.PaymentStatus;
import com.v_payment.pay.payment.domain.entity.Provider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegeneratePolicyTest {

    @DisplayName("can regenerate when order is created and all payments are regeneratable")
    @Test
    void canRegenerate() {
        RegeneratePolicy policy = RegeneratePolicy.of(
                true,
                List.of(
                        payment(PaymentStatus.READY),
                        payment(PaymentStatus.ABORTED),
                        payment(PaymentStatus.EXPIRED)
                )
        );

        assertThat(policy.canRegenerate()).isTrue();
        assertThat(policy.getRegeneratableStatuses())
                .containsExactlyInAnyOrder(PaymentStatus.READY, PaymentStatus.ABORTED, PaymentStatus.EXPIRED);
    }

    @DisplayName("cannot regenerate when order is not created")
    @Test
    void cannotRegenerateWhenOrderIsNotCreated() {
        RegeneratePolicy policy = RegeneratePolicy.of(false, List.of(payment(PaymentStatus.ABORTED)));

        assertThat(policy.canRegenerate()).isFalse();
    }

    @DisplayName("cannot regenerate without payments")
    @Test
    void cannotRegenerateWithoutPayments() {
        RegeneratePolicy policy = RegeneratePolicy.of(true, List.of());

        assertThat(policy.canRegenerate()).isFalse();
    }

    @DisplayName("cannot regenerate with not regeneratable payment")
    @Test
    void cannotRegenerateWithNotRegeneratablePayment() {
        RegeneratePolicy policy = RegeneratePolicy.of(
                true,
                List.of(payment(PaymentStatus.ABORTED), payment(PaymentStatus.IN_PROGRESS))
        );

        assertThat(policy.canRegenerate()).isFalse();
    }

    private Payment payment(PaymentStatus paymentStatus) {
        return Payment.builder()
                .provider(Provider.TOSS)
                .paymentMethod(PaymentMethod.CARD)
                .orderCode("order-code")
                .idempotencyKey("idempotency-key")
                .requestedAmount(1000L)
                .paymentStatus(paymentStatus)
                .requestedAt(LocalDateTime.now())
                .recoveryAttemptCount(0)
                .build();
    }
}
