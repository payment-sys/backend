package com.v_payment.pay.product.mq;

import com.v_payment.pay.product.entity.ProductQuantityEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Component
public class ProductQuantityConsumerMeter {
    public static final String SOURCE_READY = "ready";
    public static final String SOURCE_RETRY = "retry";

    private static final String CONSUMER_TAG_VALUE = "db";

    private final Clock clock;
    private final MeterRegistry meterRegistry;
    private final AtomicInteger running = new AtomicInteger();
    private final SourceState readyState = new SourceState();
    private final SourceState retryState = new SourceState();

    public ProductQuantityConsumerMeter(MeterRegistry meterRegistry, Clock clock) {
        this.meterRegistry = meterRegistry;
        this.clock = clock;

        Gauge.builder("product_quantity_consumer_running", running, AtomicInteger::get)
                .description("Currently running product quantity DB consumer tasks")
                .baseUnit("tasks")
                .tag("consumer", CONSUMER_TAG_VALUE)
                .register(meterRegistry);

        registerSourceGauges(SOURCE_READY, readyState);
        registerSourceGauges(SOURCE_RETRY, retryState);
    }

    public void recordRun(Runnable task) {
        running.incrementAndGet();
        try {
            task.run();
        } finally {
            running.decrementAndGet();
        }
    }

    public <T> T recordPhase(String source, String phase, Supplier<T> supplier) {
        long startNanos = System.nanoTime();
        try {
            return supplier.get();
        } finally {
            recordPhase(source, phase, System.nanoTime() - startNanos);
        }
    }

    public void recordPhase(String source, String phase, Runnable task) {
        long startNanos = System.nanoTime();
        try {
            task.run();
        } finally {
            recordPhase(source, phase, System.nanoTime() - startNanos);
        }
    }

    public void recordFetchedBatch(String source, List<ProductQuantityEvent> events, LocalDateTime now) {
        SourceState state = stateOf(source);
        state.lastBatchSize.set(events.size());
        state.lastBatchLagSeconds.set(selectedBatchLagSeconds(events, now));
        state.lastPollEpochMillis.set(clock.millis());
    }

    public void recordBatch(String source, String outcome, int eventCount, long elapsedNanos) {
        Timer.builder("product_quantity_consumer_batch_duration")
                .description("Product quantity DB consumer batch processing duration")
                .tags("consumer", CONSUMER_TAG_VALUE, "source", source, "outcome", outcome)
                .register(meterRegistry)
                .record(elapsedNanos, TimeUnit.NANOSECONDS);

        if (eventCount > 0) {
            Counter.builder("product_quantity_consumer_events")
                    .description("Product quantity DB consumer processed events")
                    .tags("consumer", CONSUMER_TAG_VALUE, "source", source, "outcome", outcome)
                    .register(meterRegistry)
                    .increment(eventCount);
        }
    }

    private void recordPhase(String source, String phase, long elapsedNanos) {
        Timer.builder("product_quantity_consumer_phase_duration")
                .description("Product quantity DB consumer phase duration")
                .tags("consumer", CONSUMER_TAG_VALUE, "source", source, "phase", phase)
                .register(meterRegistry)
                .record(elapsedNanos, TimeUnit.NANOSECONDS);
    }

    private void registerSourceGauges(String source, SourceState state) {
        Gauge.builder("product_quantity_consumer_last_batch_size", state.lastBatchSize, AtomicInteger::get)
                .description("Last fetched product quantity DB consumer batch size")
                .baseUnit("events")
                .tags("consumer", CONSUMER_TAG_VALUE, "source", source)
                .register(meterRegistry);

        Gauge.builder("product_quantity_consumer_last_batch_lag", state.lastBatchLagSeconds, AtomicLong::get)
                .description("Age of the oldest event in the last fetched product quantity DB consumer batch")
                .baseUnit("seconds")
                .tags("consumer", CONSUMER_TAG_VALUE, "source", source)
                .register(meterRegistry);

        Gauge.builder("product_quantity_consumer_last_poll_age", state, ignored -> lastPollAgeSeconds(state))
                .description("Seconds since the last product quantity DB consumer poll")
                .baseUnit("seconds")
                .tags("consumer", CONSUMER_TAG_VALUE, "source", source)
                .register(meterRegistry);
    }

    private long selectedBatchLagSeconds(List<ProductQuantityEvent> events, LocalDateTime now) {
        return events.stream()
                .map(ProductQuantityEvent::getCreatedAt)
                .filter(createdAt -> createdAt != null)
                .mapToLong(createdAt -> Math.max(0, Duration.between(createdAt, now).toSeconds()))
                .max()
                .orElse(0);
    }

    private double lastPollAgeSeconds(SourceState state) {
        long lastPollEpochMillis = state.lastPollEpochMillis.get();
        if (lastPollEpochMillis == 0) {
            return 0;
        }
        return Math.max(0, clock.millis() - lastPollEpochMillis) / 1000.0;
    }

    private SourceState stateOf(String source) {
        if (SOURCE_RETRY.equals(source)) {
            return retryState;
        }
        return readyState;
    }

    private static final class SourceState {
        private final AtomicInteger lastBatchSize = new AtomicInteger();
        private final AtomicLong lastBatchLagSeconds = new AtomicLong();
        private final AtomicLong lastPollEpochMillis = new AtomicLong();
    }
}
