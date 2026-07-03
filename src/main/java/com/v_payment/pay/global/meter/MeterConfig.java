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
    public CounterMeter requestRejectFullCounter(MeterRegistry meterRegistry) {
        return new CounterMeter(
                meterRegistry,
                "payment_request_rejected",
                "Rejected payment approval requests",
                "reason", "waiting_queue_full"
        );
    }

    @Bean
    public CounterMeter requestRejectTimeoutCounter(MeterRegistry meterRegistry) {
        return new CounterMeter(
                meterRegistry,
                "payment_request_rejected",
                "Rejected payment approval requests",
                "reason", "wait_timeout"
        );
    }

    @Bean
    public CounterMeter requestRejectInterruptCounter(MeterRegistry meterRegistry) {
        return new CounterMeter(
                meterRegistry,
                "payment_request_rejected",
                "Rejected payment approval requests",
                "reason", "interrupted"
        );
    }

    @Bean
    public DistributionSummaryMeter requestRunSummary(MeterRegistry meterRegistry) {
        return DistributionSummaryMeter.withBaseUnit(
                meterRegistry,
                "payment_request",
                "Running payment approval request concurrency",
                "requests"
        );
    }

    @Bean
    public DistributionSummaryMeter requestWaitSummary(MeterRegistry meterRegistry) {
        return DistributionSummaryMeter.withBaseUnit(
                meterRegistry,
                "payment_request_waiting",
                "Waiting payment approval requests",
                "requests"
        );
    }

    @Bean
    public DistributionSummaryMeter virtualThreadRunSummary(MeterRegistry meterRegistry) {
        return DistributionSummaryMeter.withBaseUnit(
                meterRegistry,
                "virtual_thread_running",
                "Running payment outbox virtual thread tasks",
                "tasks"
        );
    }

    @Bean
    public DistributionSummaryMeter virtualThreadWaitSummary(MeterRegistry meterRegistry) {
        return DistributionSummaryMeter.withBaseUnit(
                meterRegistry,
                "virtual_thread_limiter_waiting",
                "Waiting payment outbox virtual thread tasks",
                "tasks",
                "limiter", "payment_outbox_virtual_thread"
        );
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
    public CounterMeter paymentQueueClaimCounter(MeterRegistry meterRegistry) {
        return new CounterMeter(meterRegistry, "payment_outbox_claimed",
                "Claimed payment outbox command count");
    }

    @Bean
    public CounterMeter paymentQueueRetryCounter(MeterRegistry meterRegistry) {
        return new CounterMeter(meterRegistry, "payment_outbox_retry_scheduled",
                "Retry scheduled payment outbox command count");
    }

    @Bean
    public CounterMeter paymentQueueCompleteCounter(MeterRegistry meterRegistry) {
        return new CounterMeter(meterRegistry, "payment_outbox_completed",
                "Completed payment outbox command count");
    }

    @Bean
    public CounterMeter paymentQueueDiscardCounter(MeterRegistry meterRegistry) {
        return new CounterMeter(meterRegistry, "payment_outbox_discarded",
                "Discarded payment outbox command count");
    }

    @Bean
    public TimerMeter paymentQueueRunTimer(MeterRegistry meterRegistry) {
        return new TimerMeter(meterRegistry, "payment_outbox_scheduler_cycle",
                "Payment outbox scheduler cycle elapsed time");
    }

    @Bean
    public TimerMeter virtualThreadRunTimer(MeterRegistry meterRegistry) {
        return new TimerMeter(meterRegistry, "payment_outbox_task_elapsed",
                "Payment outbox task elapsed time");
    }
}
