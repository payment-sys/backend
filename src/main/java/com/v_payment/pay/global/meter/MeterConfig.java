package com.v_payment.pay.global.meter;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("globalMeterConfig")
public class MeterConfig {
    @Bean
    public io.micrometer.core.instrument.config.MeterFilter dashboardMetricsOnlyFilter() {
        return new MeterFilter();
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean
    public DistributionSummaryMeter resultApplyRunSummary(MeterRegistry meterRegistry) {
        return DistributionSummaryMeter.withBaseUnit(
                meterRegistry,
                "result_apply_limiter_running",
                "Running payment outbox result apply tasks",
                "tasks",
                "limiter", "payment_outbox_result_apply"
        );
    }

    @Bean
    public DistributionSummaryMeter resultApplyWaitSummary(MeterRegistry meterRegistry) {
        return DistributionSummaryMeter.withBaseUnit(
                meterRegistry,
                "result_apply_limiter_waiting",
                "Waiting payment outbox result apply tasks",
                "tasks",
                "limiter", "payment_outbox_result_apply"
        );
    }

    @Bean
    public CounterMeter paymentQueueEnqueueCounter(MeterRegistry meterRegistry) {
        return new CounterMeter(meterRegistry, "payment_outbox_enqueued",
                "Enqueued payment outbox command count");
    }

    @Bean
    public CounterMeter paymentQueueCompleteCounter(MeterRegistry meterRegistry) {
        return new CounterMeter(meterRegistry, "payment_outbox_completed",
                "Completed payment outbox command count");
    }

}
