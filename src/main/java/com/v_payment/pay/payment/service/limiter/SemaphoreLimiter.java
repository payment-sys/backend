package com.v_payment.pay.payment.service.limiter;

import java.util.concurrent.Semaphore;

public class SemaphoreLimiter implements Limiter {
    private final Semaphore semaphore;
    private final int maxConcurrentTasks;

    public SemaphoreLimiter(int maxConcurrentTasks) {
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.semaphore = new Semaphore(maxConcurrentTasks);
    }

    @Override
    public void execute(int count, Runnable task) {
        semaphore.acquireUninterruptibly(count);
        try {
            task.run();
        } finally {
            semaphore.release(count);
        }
    }

    @Override
    public void executeWithoutRelease(int count, Runnable task) {
        semaphore.acquireUninterruptibly(count);
        task.run();
    }

    @Override
    public void release() {
        semaphore.release();
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
    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }
}
