package com.v_payment.pay.payment.service;

import com.v_payment.pay.payment.config.PaymentRecoveryProperties;
import com.v_payment.pay.payment.domain.PaymentRecoveryPolicy;
import com.v_payment.pay.payment.domain.entity.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRecoveryPolicyTest {

    private final PaymentRecoveryPolicy paymentRecoveryPolicy = new PaymentRecoveryPolicy(
            new PaymentRecoveryProperties(30000L, 600L, 100)
    );

    @DisplayName("stale time uses configured stale seconds")
    @Test
    void getStaleTime() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 18, 12, 0);

        assertThat(paymentRecoveryPolicy.getStaleTime(now))
                .isEqualTo(LocalDateTime.of(2026, 9, 18, 11, 50));
    }

    @DisplayName("recoverable statuses are in progress and unknown")
    @Test
    void getRecoverableStatuses() {
        assertThat(paymentRecoveryPolicy.getRecoverableStatuses())
                .containsExactly(PaymentStatus.IN_PROGRESS, PaymentStatus.UNKNOWN);
    }

    @DisplayName("page request uses configured batch size")
    @Test
    void pageRequest() {
        assertThat(paymentRecoveryPolicy.pageRequest())
                .isEqualTo(PageRequest.of(0, 100));
    }

    @DisplayName("next attempt count treats null as zero")
    @Test
    void nextAttemptCount() {
        assertThat(paymentRecoveryPolicy.nextAttemptCount(null)).isEqualTo(1);
        assertThat(paymentRecoveryPolicy.nextAttemptCount(1)).isEqualTo(2);
    }

}
