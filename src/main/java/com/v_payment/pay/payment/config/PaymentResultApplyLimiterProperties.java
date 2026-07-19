package com.v_payment.pay.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.result-apply.limiter")
public record PaymentResultApplyLimiterProperties(
        Integer maxConcurrentTasks
) {
    private static final int DEFAULT_MAX_CONCURRENT_TASKS = 8;

    public PaymentResultApplyLimiterProperties {
        if (maxConcurrentTasks == null) {
            maxConcurrentTasks = DEFAULT_MAX_CONCURRENT_TASKS;
        }
        if (maxConcurrentTasks < 1) {
            throw new IllegalArgumentException(
                    "payment.result-apply.limiter.max-concurrent-tasks must be greater than 0");
        }
    }
}
