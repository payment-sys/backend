package com.v_payment.pay.payment.config;

import com.v_payment.pay.payment.service.limiter.Limiter;
import com.v_payment.pay.payment.service.limiter.SemaphoreLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PaymentOutboxSchedulerProperties.class)
public class PaymentOutboxConfig {
    @Bean
    public Limiter virtualThreadLimiter(
            @Value("${payment.outbox.limiter.max-virtual-thread-tasks}") int maxVirtualThreadTasks,
            MeterRegistry meterRegistry
    ) {
        return new SemaphoreLimiter(maxVirtualThreadTasks, "payment_outbox_virtual_thread", meterRegistry);
    }

    @Bean
    public Limiter resultApplyLimiter(
            @Value("${payment.outbox.limiter.max-result-apply-tasks}") int maxResultApplyTasks,
            MeterRegistry meterRegistry
    ) {
        return new SemaphoreLimiter(maxResultApplyTasks, "payment_outbox_result_apply", meterRegistry);
    }
}
