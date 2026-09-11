package com.v_payment.pay.product.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RetryPolicyTest {
    private final RetryPolicy retryPolicy = new RetryPolicy();

    @DisplayName("재시도 횟수에 따라 다음 시도 시간을 지수적으로 늦춘다")
    @Test
    void nextAttemptTime() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 9, 10, 0, 0);

        // when & then
        assertThat(retryPolicy.nextAttemptTime(now, 0)).isEqualTo(now.plusSeconds(1));
        assertThat(retryPolicy.nextAttemptTime(now, 1)).isEqualTo(now.plusSeconds(2));
        assertThat(retryPolicy.nextAttemptTime(now, 2)).isEqualTo(now.plusSeconds(4));
    }

    @DisplayName("재시도 대기 시간은 최대 30초를 넘지 않는다")
    @Test
    void nextAttemptTimeWithMaxDelay() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 9, 10, 0, 0);

        // when
        LocalDateTime nextAttemptTime = retryPolicy.nextAttemptTime(now, 10);

        // then
        assertThat(nextAttemptTime).isEqualTo(now.plusSeconds(30));
    }
}
