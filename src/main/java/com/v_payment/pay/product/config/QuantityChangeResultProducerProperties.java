package com.v_payment.pay.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "product.quantity-change-result-producer")
public record QuantityChangeResultProducerProperties(
        String topic
) {
    private static final String DEFAULT_TOPIC = "quantity-change-results";

    public QuantityChangeResultProducerProperties {
        if (topic == null || topic.isBlank()) {
            topic = DEFAULT_TOPIC;
        }
    }
}
