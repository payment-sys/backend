package com.v_payment.pay.product.mq;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class RetryPolicy {
    private static final Duration INITIAL_DELAY = Duration.ofSeconds(1);
    private static final Duration MAX_DELAY = Duration.ofSeconds(30);

    public LocalDateTime nextAttemptTime(LocalDateTime now, int retryCount) {
        long delaySeconds = INITIAL_DELAY.toSeconds() * (1L << Math.min(retryCount, 5));
        Duration delay = Duration.ofSeconds(Math.min(delaySeconds, MAX_DELAY.toSeconds()));

        return now.plus(delay);
    }
}
