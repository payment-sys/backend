package com.v_payment.pay.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "product.quantity-event.consumer")
public record ProductQuantityEventConsumerProperties(
        Integer batchSize
) {
    private static final int DEFAULT_BATCH_SIZE = 200;

    public ProductQuantityEventConsumerProperties {
        if (batchSize == null) {
            batchSize = DEFAULT_BATCH_SIZE;
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("product.quantity-event.consumer.batch-size는 1 이상이어야 합니다.");
        }
    }
}
