package com.v_payment.pay.payment.limiter;

import com.v_payment.pay.payment.entity.Payment;

public interface Limiter {
    default void execute(Runnable task) {
        execute(1, task);
    }

    void execute(int count, Runnable task);

    void executeWithoutRelease(int count, Runnable task);

    void release();

    int getAvailableCount();

    int getRunningCount();

    int getWaitingCount();

    int getMaxConcurrentTasks();
}
