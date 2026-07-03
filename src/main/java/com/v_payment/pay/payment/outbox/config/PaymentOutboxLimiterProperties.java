package com.v_payment.pay.payment.outbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.outbox.limiter")
public record PaymentOutboxLimiterProperties(
        int maxVirtualThreadTasks,
        int maxResultApplyTasks
) {
    public PaymentOutboxLimiterProperties {
        if (maxVirtualThreadTasks < 1) {
            throw new IllegalArgumentException("payment.outbox.limiter.max-virtual-thread-tasks must be greater than 0");
        }
        if (maxResultApplyTasks < 1) {
            throw new IllegalArgumentException("payment.outbox.limiter.max-result-apply-tasks must be greater than 0");
        }
    }
}
