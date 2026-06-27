package com.v_payment.pay.payment.config;

import com.v_payment.pay.payment.service.limiter.Limiter;
import com.v_payment.pay.payment.service.limiter.SemaphoreLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PaymentOutboxSchedulerProperties.class)
public class PaymentOutboxConfig {
    @Bean
    public Limiter virtualThreadLimiter(
            @Value("${payment.outbox.limiter.max-virtual-thread-tasks}") int maxVirtualThreadTasks
    ) {
        return new SemaphoreLimiter(maxVirtualThreadTasks);
    }

    @Bean
    public Limiter resultApplyLimiter(
            @Value("${payment.outbox.limiter.max-result-apply-tasks}") int maxResultApplyTasks
    ) {
        return new SemaphoreLimiter(maxResultApplyTasks);
    }
}
