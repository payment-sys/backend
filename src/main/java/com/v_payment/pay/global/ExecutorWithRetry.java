package com.v_payment.pay.global;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

@Slf4j
@RequiredArgsConstructor
public class ExecutorWithRetry<T> {
    private Callable<T> task;
    private final List<Predicate<T>> continueConditions = new LinkedList<>();
    private int maxAttempts;
    private long delayMillis;
    private Runnable recovery;

    public T execute() {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be at least 1.");
        if (delayMillis < 0) throw new IllegalArgumentException("delayMillis must be 0 or greater.");

        T lastResult = null;
        Exception exception = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            log.info("approval retry attempt = {}", attempt);
            try {
                lastResult = task.call();
                T finalLastResult = lastResult;
                boolean isContinue = continueConditions.stream().anyMatch(cond -> cond.test(finalLastResult));
                if (!isContinue) return lastResult;
            } catch (Exception e) {
                log.warn("unknown exception occurred during retry task.");
                exception = e;
                throw new RuntimeException(exception);
            }
            if (attempt < maxAttempts) sleep(delayMillis);
        }

        try {
            if (recovery != null) recovery.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (exception != null) throw new RuntimeException(exception);

        throw new IllegalStateException("retry maxAttempts=" + maxAttempts + " exhausted, lastResult=" + lastResult);
    }

    private static void sleep(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", e);
        }
    }

    private ExecutorWithRetry(Callable<T> task) {
        this.task = Objects.requireNonNull(task, "task must not be null.");
    }

    public static <T> ExecutorWithRetry<T> task(Callable<T> task) {
        return new ExecutorWithRetry<>(task);
    }

    public ExecutorWithRetry<T> continueCondition(Predicate<T> continueCondition) {
        this.continueConditions.add(continueCondition);
        return this;
    }

    public ExecutorWithRetry<T> maxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
        return this;
    }

    public ExecutorWithRetry<T> delayMillis(long delayMillis) {
        this.delayMillis = delayMillis;
        return this;
    }

    public ExecutorWithRetry<T> recovery(Runnable recovery) {
        this.recovery = recovery;
        return this;
    }
}
