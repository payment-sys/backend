package com.v_payment.pay.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "product.kafka.producers.quantity-change-result")
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
