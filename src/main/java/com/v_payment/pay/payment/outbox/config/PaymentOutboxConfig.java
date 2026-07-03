package com.v_payment.pay.payment.outbox.config;

import com.v_payment.pay.global.meter.DistributionSummaryMeter;
import com.v_payment.pay.payment.outbox.limiter.Limiter;
import com.v_payment.pay.payment.outbox.limiter.SemaphoreLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class PaymentOutboxConfig {
    private final PaymentOutboxLimiterProperties properties;
    private final DistributionSummaryMeter virtualThreadRunSummary;
    private final DistributionSummaryMeter virtualThreadWaitSummary;
    private final DistributionSummaryMeter resultApplyRunSummary;
    private final DistributionSummaryMeter resultApplyWaitSummary;

    @Bean
    public Limiter paymentOutboxVirtualThreadLimiter() {
        return new SemaphoreLimiter(
                properties.maxVirtualThreadTasks(),
                virtualThreadWaitSummary,
                virtualThreadRunSummary
        );
    }

    @Bean
    public Limiter paymentOutboxResultApplyLimiter() {
        return new SemaphoreLimiter(
                properties.maxResultApplyTasks(),
                resultApplyWaitSummary,
                resultApplyRunSummary
        );
    }
}
