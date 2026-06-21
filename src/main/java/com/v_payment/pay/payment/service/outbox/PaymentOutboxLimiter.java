package com.v_payment.pay.payment.service.outbox;

import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

@Component
public class PaymentOutboxLimiter {
    private static final int MAX_CONCURRENT_TASKS = 200;
    private static final Semaphore semaphore = new Semaphore(MAX_CONCURRENT_TASKS);

    public void acquire(int count) {
        semaphore.acquireUninterruptibly(count);
    }

    public void release() {
        semaphore.release();
    }

    public void release(int count) {
        semaphore.release(count);
    }

    public int getAvailableCount() {
        return semaphore.availablePermits();
    }

    public int getRunningCount() {
        return MAX_CONCURRENT_TASKS - semaphore.availablePermits();
    }

    public int getMaxConcurrentTasks() {
        return MAX_CONCURRENT_TASKS;
    }
}
