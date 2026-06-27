package com.v_payment.pay.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.outbox.scheduler")
public record PaymentOutboxSchedulerProperties(
        int minBatchSize,
        int maxBatchSize,
        long minPollingDelayMs,
        long maxPollingDelayMs,
        int pollingDelayMultiplier
) {}
