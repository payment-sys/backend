package com.v_payment.pay.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "order.expiration")
public record OrderExpirationProperties(
        Long fixedDelayMs,
        Long expireAfterSeconds,
        Integer batchSize
) {
    private static final long DEFAULT_FIXED_DELAY_MS = 30000L;
    private static final long DEFAULT_EXPIRE_AFTER_SECONDS = 600L;
    private static final int DEFAULT_BATCH_SIZE = 100;

    public OrderExpirationProperties {
        if (fixedDelayMs == null) {
            fixedDelayMs = DEFAULT_FIXED_DELAY_MS;
        }
        if (expireAfterSeconds == null) {
            expireAfterSeconds = DEFAULT_EXPIRE_AFTER_SECONDS;
        }
        if (batchSize == null) {
            batchSize = DEFAULT_BATCH_SIZE;
        }
        if (fixedDelayMs < 1) {
            throw new IllegalArgumentException("order.expiration.fixed-delay-ms는 0보다 커야 합니다.");
        }
        if (expireAfterSeconds < 1) {
            throw new IllegalArgumentException("order.expiration.expire-after-seconds는 0보다 커야 합니다.");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("order.expiration.batch-size는 0보다 커야 합니다.");
        }
    }
}
