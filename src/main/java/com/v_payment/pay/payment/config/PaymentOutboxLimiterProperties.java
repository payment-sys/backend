package com.v_payment.pay.payment.config;

import com.v_payment.pay.payment.limiter.Limiter;
import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.infra.Result;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.outbox.limiter")
public record PaymentOutboxLimiterProperties(
        int maxResultApplyTasks
) {
    public PaymentOutboxLimiterProperties {
        if (maxResultApplyTasks < 1) {
            throw new IllegalArgumentException("payment.outbox.limiter.max-result-apply-tasks must be greater than 0");
        }
    }
}
