package com.v_payment.pay.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "order.quantity-change-result-aggregation")
public record QuantityChangeResultAggregationProperties(
        String inputTopic,
        String outputTopic
) {
    public QuantityChangeResultAggregationProperties {
        if (inputTopic == null || inputTopic.isBlank()) {
            throw new IllegalArgumentException("order.quantity-change-result-aggregation.input-topic is required");
        }
        if (outputTopic == null || outputTopic.isBlank()) {
            throw new IllegalArgumentException("order.quantity-change-result-aggregation.output-topic is required");
        }
    }
}