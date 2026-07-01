package com.v_payment.pay.payment.service.limiter;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.Semaphore;

public class SemaphoreLimiter implements Limiter {
    private final Semaphore semaphore;
    private final int maxConcurrentTasks;
    private final DistributionSummary waitingDepthSummary;

    public SemaphoreLimiter(int maxConcurrentTasks) {
        this(maxConcurrentTasks, null, null);
    }

    public SemaphoreLimiter(int maxConcurrentTasks, String limiterName, MeterRegistry meterRegistry) {
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.semaphore = new Semaphore(maxConcurrentTasks, true);
        if (meterRegistry == null || limiterName == null) {
            this.waitingDepthSummary = null;
            return;
        }
        this.waitingDepthSummary = DistributionSummary.builder("payment_limiter_waiting_depth")
                .description("Observed number of tasks waiting for limiter permits")
                .baseUnit("tasks")
                .tag("limiter", limiterName)
                .register(meterRegistry);
    }

    @Override
    public void execute(int count, Runnable task) {
        acquire(count);
        try {
            task.run();
        } finally {
            release(count);
        }
    }

    @Override
    public void executeWithoutRelease(int count, Runnable task) {
        acquire(count);
        task.run();
    }

    @Override
    public void release() {
        release(1);
    }

    @Override
    public int getAvailableCount() {
        return semaphore.availablePermits();
    }

    @Override
    public int getRunningCount() {
        return maxConcurrentTasks - semaphore.availablePermits();
    }

    @Override
    public int getWaitingCount() {
        return semaphore.getQueueLength();
    }

    @Override
    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    private void acquire(int count) {
        recordWaitingDepthBeforeAcquire(count);
        semaphore.acquireUninterruptibly(count);
        recordWaitingDepth();
    }

    private void release(int count) {
        semaphore.release(count);
        recordWaitingDepth();
    }

    private void recordWaitingDepthBeforeAcquire(int count) {
        int waiting = semaphore.getQueueLength();
        if (semaphore.availablePermits() < count) {
            waiting++;
        }
        recordWaitingDepth(waiting);
    }

    private void recordWaitingDepth() {
        recordWaitingDepth(semaphore.getQueueLength());
    }

    private void recordWaitingDepth(int waiting) {
        if (waitingDepthSummary != null) {
            waitingDepthSummary.record(Math.max(waiting, 0));
        }
    }
}
