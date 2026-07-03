package com.v_payment.pay.global.meter;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;

public class DistributionSummaryMeter {
    private final DistributionSummary distributionSummary;

    public DistributionSummaryMeter(MeterRegistry meterRegistry, String name, String description, String... tags) {
        this(meterRegistry, name, description, null, tags, true);
    }

    public static DistributionSummaryMeter withBaseUnit(
            MeterRegistry meterRegistry,
            String name,
            String description,
            String baseUnit,
            String... tags
    ) {
        return new DistributionSummaryMeter(meterRegistry, name, description, baseUnit, tags, true);
    }

    private DistributionSummaryMeter(
            MeterRegistry meterRegistry,
            String name,
            String description,
            String baseUnit,
            String[] tags,
            boolean ignored
    ) {
        DistributionSummary.Builder builder = DistributionSummary.builder(name)
                .description(description)
                .tags(tags);
        if (baseUnit != null) {
            builder.baseUnit(baseUnit);
        }
        this.distributionSummary = builder.register(meterRegistry);
    }

    public void record() {
        this.distributionSummary.record(1);
    }

    public void record(int value) {
        record((double) value);
    }

    public void record(double value) {
        if (value < 0) {
            throw new IllegalArgumentException("distribution summary value must not be negative");
        }
        this.distributionSummary.record(value);
    }
}
