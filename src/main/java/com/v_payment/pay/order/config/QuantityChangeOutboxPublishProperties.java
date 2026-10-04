package com.v_payment.pay.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "order.quantity-change-outbox-scheduler")
public record QuantityChangeOutboxPublishProperties(
        Long fixedDelayMs,
        Integer batchSize,
        Long retryDelaySeconds
) {
    private static final long DEFAULT_FIXED_DELAY_MS = 30000L;
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final long DEFAULT_RETRY_DELAY_SECONDS = 30L;

    public QuantityChangeOutboxPublishProperties {
        if (fixedDelayMs == null) {
            fixedDelayMs = DEFAULT_FIXED_DELAY_MS;
        }
        if (batchSize == null) {
            batchSize = DEFAULT_BATCH_SIZE;
        }
        if (retryDelaySeconds == null) {
            retryDelaySeconds = DEFAULT_RETRY_DELAY_SECONDS;
        }
        if (fixedDelayMs < 1) {
            throw new IllegalArgumentException("order.quantity-change-outbox-scheduler.fixed-delay-ms must be greater than 0");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("order.quantity-change-outbox-scheduler.batch-size must be greater than 0");
        }
        if (retryDelaySeconds < 0) {
            throw new IllegalArgumentException("order.quantity-change-outbox-scheduler.retry-delay-seconds must not be negative");
        }
    }
}
