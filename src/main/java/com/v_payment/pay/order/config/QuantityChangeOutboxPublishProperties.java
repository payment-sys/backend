package com.v_payment.pay.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "order.quantity-changes.outbox-publish")
public record QuantityChangeOutboxPublishProperties(
        Long fixedDelayMs,
        Integer batchSize
) {
    private static final long DEFAULT_FIXED_DELAY_MS = 30000L;
    private static final int DEFAULT_BATCH_SIZE = 100;

    public QuantityChangeOutboxPublishProperties {
        if (fixedDelayMs == null) {
            fixedDelayMs = DEFAULT_FIXED_DELAY_MS;
        }
        if (batchSize == null) {
            batchSize = DEFAULT_BATCH_SIZE;
        }
        if (fixedDelayMs < 1) {
            throw new IllegalArgumentException("order.quantity-changes.outbox-publish.fixed-delay-ms must be greater than 0");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("order.quantity-changes.outbox-publish.batch-size must be greater than 0");
        }
    }
}
