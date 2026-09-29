package com.v_payment.pay.order.config;

import io.opentelemetry.context.Context;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class OrderExecutorConfig {
    @Bean
    ExecutorService quantityChangeEventExecutorService() {
        return Context.taskWrapping(Executors.newVirtualThreadPerTaskExecutor());
    }
}
