package com.v_payment.pay.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "order.quantity-change-summary-consumer")
public record QuantityChangeSummaryConsumerProperties(
        String topic,
        String groupId
) {
    public QuantityChangeSummaryConsumerProperties {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("order.quantity-change-summary-consumer.topic is required");
        }
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("order.quantity-change-summary-consumer.group-id is required");
        }
    }
}
