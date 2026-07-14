package com.v_payment.pay.global.meter;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("globalMeterConfig")
public class MeterConfig {
    @Bean
    public io.micrometer.core.instrument.config.MeterFilter dashboardMetricsOnlyFilter() {
        return new MeterFilter();
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

}
