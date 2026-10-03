package com.v_payment.pay.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.toss")
public record TossPaymentProperties(
        String uri,
        String secret,
        String contentType,
        Long timeout
) {}
