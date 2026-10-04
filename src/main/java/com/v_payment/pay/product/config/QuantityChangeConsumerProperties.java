package com.v_payment.pay.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "product.quantity-change-consumer")
public record QuantityChangeConsumerProperties(
        String topic,
        String groupId
) {
}
