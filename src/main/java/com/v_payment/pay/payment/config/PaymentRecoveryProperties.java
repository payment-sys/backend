package com.v_payment.pay.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.recovery")
public record PaymentRecoveryProperties(
        Long fixedDelayMs,
        Long staleAfterSeconds,
        Integer batchSize
) {
    private static final long DEFAULT_FIXED_DELAY_MS = 30000L;
    private static final long DEFAULT_STALE_AFTER_SECONDS = 600L;
    private static final int DEFAULT_BATCH_SIZE = 100;

    public PaymentRecoveryProperties {
        if (fixedDelayMs == null) {
            fixedDelayMs = DEFAULT_FIXED_DELAY_MS;
        }
        if (staleAfterSeconds == null) {
            staleAfterSeconds = DEFAULT_STALE_AFTER_SECONDS;
        }
        if (batchSize == null) {
            batchSize = DEFAULT_BATCH_SIZE;
        }
        if (fixedDelayMs < 1) {
            throw new IllegalArgumentException("payment.recovery.fixed-delay-ms must be greater than 0");
        }
        if (staleAfterSeconds < 1) {
            throw new IllegalArgumentException("payment.recovery.stale-after-seconds must be greater than 0");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("payment.recovery.batch-size must be greater than 0");
        }
    }
}
