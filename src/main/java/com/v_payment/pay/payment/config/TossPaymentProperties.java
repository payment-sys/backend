package com.v_payment.pay.payment.config;

import com.v_payment.pay.payment.entity.Payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toss.payment")
public record TossPaymentProperties(
        String uri,
        String secret,
        String contentType,
        Long timeout
) {}
