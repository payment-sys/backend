package com.v_payment.pay.global.meter;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;

import java.time.Duration;

public class MeterFilter implements io.micrometer.core.instrument.config.MeterFilter {
    @Override
    public MeterFilterReply accept(Meter.Id id) {
        return isDashboardMetric(id.getName()) ? MeterFilterReply.NEUTRAL : MeterFilterReply.DENY;
    }

    @Override
    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
        String name = id.getName();
        if (name.equals("http.server.requests")) {
            return buckets(
                    config,
                    Duration.ofMillis(100).toNanos(),
                    Duration.ofMillis(300).toNanos(),
                    Duration.ofMillis(600).toNanos(),
                    Duration.ofMillis(900).toNanos(),
                    Duration.ofMillis(1200).toNanos(),
                    Duration.ofMillis(1500).toNanos(),
                    Duration.ofSeconds(2).toNanos()
            );
        }
        if (name.equals("virtual_thread_running")) {
            return buckets(config, 1, 10, 30, 50, 100, 200, 300);
        }
        if (name.equals("virtual_thread_limiter_waiting")) {
            return buckets(config, 1, 10, 30, 50, 100, 200, 300);
        }
        return config;
    }

    private static boolean isDashboardMetric(String name) {
        return name.equals("http.server.requests")
                || name.equals("http.server.requests.active")
                || name.equals("pay.api")
                || name.startsWith("hikaricp.connections")
                || name.startsWith("tomcat.threads")
                || name.startsWith("tomcat.connections")
                || name.startsWith("virtual_thread")
                || name.equals("jvm.memory.used")
                || name.equals("jvm.memory.committed")
                || name.equals("jvm.gc.pause")
                || name.equals("jvm.threads.live")
                || name.equals("jvm.threads.peak")
                || name.equals("jvm.threads.daemon")
                || name.equals("process.cpu.usage")
                || name.equals("system.cpu.usage");
    }

    private static DistributionStatisticConfig buckets(DistributionStatisticConfig config, double... buckets) {
        return DistributionStatisticConfig.builder()
                .serviceLevelObjectives(buckets)
                .build()
                .merge(config);
    }
}
