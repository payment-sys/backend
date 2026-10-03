package com.v_payment.pay.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "order.quantity-change-producer")
public record QuantityChangeProducerProperties(
        String topic
) {
}
