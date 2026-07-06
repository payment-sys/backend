package com.v_payment.pay.payment.config;

import com.v_payment.pay.payment.limiter.Limiter;
import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.limiter.SemaphoreLimiter;

import com.v_payment.pay.global.meter.DistributionSummaryMeter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class PaymentOutboxConfig {
    private final PaymentOutboxLimiterProperties properties;
    private final DistributionSummaryMeter resultApplyRunSummary;
    private final DistributionSummaryMeter resultApplyWaitSummary;

    @Bean
    public Limiter paymentOutboxResultApplyLimiter() {
        return new SemaphoreLimiter(
                properties.maxResultApplyTasks(),
                resultApplyWaitSummary,
                resultApplyRunSummary
        );
    }
}
