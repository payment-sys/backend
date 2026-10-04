package com.v_payment.pay.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "product.quantity-change-result-outbox-scheduler")
public record QuantityChangeResultOutboxPublishProperties(
        Long fixedDelayMs,
        Integer batchSize,
        Long retryDelaySeconds
) {
    private static final long DEFAULT_FIXED_DELAY_MS = 30000L;
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final long DEFAULT_RETRY_DELAY_SECONDS = 30L;

    public QuantityChangeResultOutboxPublishProperties {
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
            throw new IllegalArgumentException("product.quantity-change-result-outbox-scheduler.fixed-delay-ms must be greater than 0");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("product.quantity-change-result-outbox-scheduler.batch-size must be greater than 0");
        }
        if (retryDelaySeconds < 0) {
            throw new IllegalArgumentException("product.quantity-change-result-outbox-scheduler.retry-delay-seconds must not be negative");
        }
    }
}
