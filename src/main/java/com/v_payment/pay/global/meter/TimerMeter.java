package com.v_payment.pay.global.meter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class TimerMeter {
    private final Timer timer;

    public TimerMeter(MeterRegistry meterRegistry, String name, String description, String... tags) {
        this.timer = Timer.builder(name)
                .description(description)
                .tags(tags)
                .register(meterRegistry);
    }

    public void record(Duration duration) {
        this.timer.record(duration);
    }

    public void record(long amount, TimeUnit unit) {
        this.timer.record(amount, unit);
    }

    public void record(Runnable task) {
        this.timer.record(task);
    }

    public <T> T record(Supplier<T> supplier) {
        return this.timer.record(supplier);
    }

    public <T> T recordCallable(Callable<T> callable) throws Exception {
        return this.timer.recordCallable(callable);
    }
}
