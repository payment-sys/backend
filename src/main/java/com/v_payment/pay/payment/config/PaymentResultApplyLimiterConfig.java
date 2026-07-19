package com.v_payment.pay.payment.config;

import com.v_payment.pay.global.meter.DistributionSummaryMeter;
import com.v_payment.pay.payment.limiter.Limiter;
import com.v_payment.pay.payment.limiter.SemaphoreLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentResultApplyLimiterConfig {
    @Bean
    public Limiter paymentResultApplyLimiter(
            PaymentResultApplyLimiterProperties properties,
            MeterRegistry meterRegistry
    ) {
        return new SemaphoreLimiter(
                properties.maxConcurrentTasks(),
                DistributionSummaryMeter.withBaseUnit(
                        meterRegistry,
                        "result_apply_limiter_waiting",
                        "Waiting payment result apply tasks",
                        "tasks",
                        "limiter", "payment_result_apply"
                ),
                DistributionSummaryMeter.withBaseUnit(
                        meterRegistry,
                        "result_apply_limiter_running",
                        "Running payment result apply tasks",
                        "tasks",
                        "limiter", "payment_result_apply"
                )
        );
    }
}
