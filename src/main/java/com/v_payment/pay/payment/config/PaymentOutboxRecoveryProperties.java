package com.v_payment.pay.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.outbox.recovery")
public record PaymentOutboxRecoveryProperties(
        long staleAfterSeconds,
        int batchSize
) {
    public PaymentOutboxRecoveryProperties {
        if (staleAfterSeconds < 1) {
            throw new IllegalArgumentException("payment.outbox.recovery.stale-after-seconds must be greater than 0");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("payment.outbox.recovery.batch-size must be greater than 0");
        }
    }
}
