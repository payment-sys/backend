package com.v_payment.pay.payment.service.outbox;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PaymentOutboxMetric {

    private static Counter enqueuedCounter;
    private static Counter claimedCounter;
    private static Counter retryScheduledCounter;
    private static Counter completedCounter;
    private static Counter discardedCounter;

    public PaymentOutboxMetric(MeterRegistry meterRegistry) {
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
}
