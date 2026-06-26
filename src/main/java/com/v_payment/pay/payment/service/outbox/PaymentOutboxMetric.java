package com.v_payment.pay.payment.service.outbox;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class PaymentOutboxMetric {

    private static Counter enqueuedCounter;
    private static Counter claimedCounter;
    private static Counter retryScheduledCounter;
    private static Counter completedCounter;
    private static Counter discardedCounter;
    private static Timer schedulerCycleTimer;
    private static Timer taskElapsedTimer;

    public PaymentOutboxMetric(MeterRegistry meterRegistry, PaymentOutboxLimiter paymentOutboxLimiter) {
        enqueuedCounter = Counter.builder("payment_outbox_enqueued")
                .description("Total number of payment outbox commands enqueued")
                .register(meterRegistry);

        claimedCounter = Counter.builder("payment_outbox_claimed")
                .description("Total number of payment outbox commands claimed for processing")
                .register(meterRegistry);

        retryScheduledCounter = Counter.builder("payment_outbox_retry_scheduled")
                .description("Total number of payment outbox commands scheduled for retry")
                .register(meterRegistry);

        completedCounter = Counter.builder("payment_outbox_completed")
                .description("Total number of payment outbox commands completed")
                .register(meterRegistry);

        discardedCounter = Counter.builder("payment_outbox_discarded")
                .description("Total number of payment outbox commands discarded or moved to DLQ")
                .register(meterRegistry);

        Gauge.builder("payment_outbox_running_tasks", paymentOutboxLimiter, PaymentOutboxLimiter::getRunningCount)
                .description("Current number of payment outbox virtual-thread tasks in progress")
                .register(meterRegistry);

        Gauge.builder("payment_outbox_available_slots", paymentOutboxLimiter, PaymentOutboxLimiter::getAvailableCount)
                .description("Current number of available payment outbox processing slots")
                .register(meterRegistry);

        Gauge.builder("payment_outbox_max_concurrent_tasks", paymentOutboxLimiter, PaymentOutboxLimiter::getMaxConcurrentTasks)
                .description("Maximum number of concurrent payment outbox processing tasks")
                .register(meterRegistry);

        schedulerCycleTimer = Timer.builder("payment_outbox_scheduler_cycle")
                .description("Time spent in one payment outbox scheduler cycle with tasks")
                .register(meterRegistry);

        taskElapsedTimer = Timer.builder("payment_outbox_task_elapsed")
                .description("Time from payment outbox polling cycle start to task completion")
                .register(meterRegistry);
    }

    public static void incrementEnqueued() {
        enqueuedCounter.increment();
    }

    public static void incrementClaimed(long count) {
        if (count > 0) {
            claimedCounter.increment(count);
        }
    }

    public static void incrementRetryScheduled(long count) {
        if (count > 0) {
            retryScheduledCounter.increment(count);
        }
    }

    public static void incrementCompleted(long count) {
        if (count > 0) {
            completedCounter.increment(count);
        }
    }

    public static void incrementDiscarded(long count) {
        if (count > 0) {
            discardedCounter.increment(count);
        }
    }

    public static void recordSchedulerCycle(long elapsedNanos) {
        schedulerCycleTimer.record(Duration.ofNanos(elapsedNanos));
    }

    public static void recordTaskElapsed(long elapsedNanos) {
        taskElapsedTimer.record(Duration.ofNanos(elapsedNanos));
    }
}
