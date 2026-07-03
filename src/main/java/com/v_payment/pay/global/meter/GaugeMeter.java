package com.v_payment.pay.global.meter;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.function.ToDoubleFunction;

public class GaugeMeter {
    private final Gauge gauge;

    public <T> GaugeMeter(
            MeterRegistry meterRegistry,
            String name,
            String description,
            T obj,
            ToDoubleFunction<T> valueFunction,
            String... tags
    ) {
        this.gauge = Gauge.builder(name, obj, valueFunction)
                .description(description)
                .tags(tags)
                .register(meterRegistry);
    }

    public double value() {
        return this.gauge.value();
    }
}
