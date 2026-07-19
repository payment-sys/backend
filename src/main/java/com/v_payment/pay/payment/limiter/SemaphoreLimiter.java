package com.v_payment.pay.payment.limiter;

import com.v_payment.pay.global.meter.DistributionSummaryMeter;

import java.util.concurrent.Semaphore;

public class SemaphoreLimiter implements Limiter {
    private final Semaphore semaphore;
    private final int maxConcurrentTasks;
    private final DistributionSummaryMeter waitSummary;
    private final DistributionSummaryMeter runSummary;

    public SemaphoreLimiter(
            int maxConcurrentTasks,
            DistributionSummaryMeter waitSummary,
            DistributionSummaryMeter runSummary
    ) {
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.semaphore = new Semaphore(maxConcurrentTasks, true);
        this.waitSummary = waitSummary;
        this.runSummary = runSummary;
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
        recordWaitBeforeAcquire(count);
        semaphore.acquireUninterruptibly(count);
        recordRun();
        recordWait();
    }

    private void release(int count) {
        semaphore.release(count);
        recordRun();
        recordWait();
    }

    private void recordWaitBeforeAcquire(int count) {
        int waiting = semaphore.getQueueLength();
        if (semaphore.availablePermits() < count) {
            waiting++;
        }
        recordWait(waiting);
    }

    private void recordWait() {
        recordWait(semaphore.getQueueLength());
    }

    private void recordWait(int waiting) {
        waitSummary.record(Math.max(waiting, 0));
    }

    private void recordRun() {
        runSummary.record(getRunningCount());
    }
}
