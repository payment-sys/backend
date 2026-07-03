package com.v_payment.pay.global.meter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public class CounterMeter {
    private final Counter counter;

    public CounterMeter(MeterRegistry meterRegistry, String name, String description, String... tags) {
        this.counter = Counter.builder(name)
                .description(description)
                .tags(tags)
                .register(meterRegistry);
    }

    public void increment() {
        this.counter.increment();
    }

    public void increment(double amount) {
        if (amount == 0) return;
        this.counter.increment(amount);
    }
}
