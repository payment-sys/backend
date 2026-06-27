package com.v_payment.pay.payment.service.outbox;

import com.v_payment.pay.payment.config.PaymentOutboxSchedulerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TriggerContext;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SchedulerManager {
    private final PaymentOutboxSchedulerProperties schedulerProperties;
    private volatile long currentSchedulingDelayMs;

    public int calculateBatchableSize(int availableBatchSize) {
        int batchableSize = Math.min(availableBatchSize, maxBatchSize());
        if (batchableSize >= minBatchSize()) return batchableSize;
        return 0;
    }

    public void applySchedulingDelay(int batchResultSize) {
        if (batchResultSize > 0) { resetSchedulingDelay(); return; }
        increaseSchedulingDelay();
    }

    public long resetSchedulingDelay() {
        currentSchedulingDelayMs = minPollingDelayMs();
        return currentSchedulingDelayMs;
    }

    public Instant nextExecution(TriggerContext triggerContext) {
        return Instant.now().plusMillis(currentSchedulingDelayOrMin());
    }

    private long increaseSchedulingDelay() {
        long nextDelayMs = currentSchedulingDelayOrMin() * schedulerProperties.pollingDelayMultiplier();
        currentSchedulingDelayMs = Math.min(maxPollingDelayMs(), Math.max(minPollingDelayMs(), nextDelayMs));
        return currentSchedulingDelayMs;
    }

    private long currentSchedulingDelayOrMin() {
        if(currentSchedulingDelayMs > 0) return currentSchedulingDelayMs;
        return minPollingDelayMs();
    }

    private int minBatchSize() {
        return schedulerProperties.minBatchSize();
    }

    private int maxBatchSize() {
        return schedulerProperties.maxBatchSize();
    }

    private long minPollingDelayMs() {
        return schedulerProperties.minPollingDelayMs();
    }

    private long maxPollingDelayMs() {
        return schedulerProperties.maxPollingDelayMs();
    }
}
