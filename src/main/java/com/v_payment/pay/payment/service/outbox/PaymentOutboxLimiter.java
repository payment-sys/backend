package com.v_payment.pay.payment.service.outbox;

import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

@Component
public class PaymentOutboxLimiter {
    private static final Semaphore semaphore = new Semaphore(1000);

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
}
