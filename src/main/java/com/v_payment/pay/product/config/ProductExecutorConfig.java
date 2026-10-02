package com.v_payment.pay.product.config;

import io.opentelemetry.context.Context;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ProductExecutorConfig {
    @Bean
    ExecutorService quantityChangeResultEventExecutorService() {
        return Context.taskWrapping(Executors.newVirtualThreadPerTaskExecutor());
    }
}
