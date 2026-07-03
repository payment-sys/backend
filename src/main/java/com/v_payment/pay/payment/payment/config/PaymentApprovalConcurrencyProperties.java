package com.v_payment.pay.payment.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "payment.approval.concurrency")
public record PaymentApprovalConcurrencyProperties(
        Integer min,
        Integer max,
        Integer maxWait,
        Duration maxWaitTime
) {
    public PaymentApprovalConcurrencyProperties {
        if (min == null) {
            min = 0;
        }
        if (max == null) {
            max = 24;
        }
        if (maxWait == null) {
            maxWait = 0;
        }
        if (maxWaitTime == null) {
            maxWaitTime = Duration.ofMillis(100);
        }
        if (max < 1) {
            throw new IllegalArgumentException("payment.approval.concurrency.max must be greater than 0");
        }
        if (min < 0) {
            throw new IllegalArgumentException("payment.approval.concurrency.min must not be negative");
        }
        if (min >= max) {
            throw new IllegalArgumentException("payment.approval.concurrency.min must be less than max");
        }
        if (maxWait < 0) {
            throw new IllegalArgumentException("payment.approval.concurrency.max-wait must not be negative");
        }
        if (maxWaitTime.isNegative()) {
            throw new IllegalArgumentException("payment.approval.concurrency.max-wait-time must not be negative");
        }
    }
}
