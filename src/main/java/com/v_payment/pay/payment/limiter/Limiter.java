package com.v_payment.pay.payment.limiter;

public interface Limiter {
    default void execute(Runnable task) {
        execute(1, task);
    }

    void execute(int count, Runnable task);

    int getAvailableCount();

    int getRunningCount();

    int getWaitingCount();

    int getMaxConcurrentTasks();
}
