package com.v_payment.pay.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class PaymentExecutorConfig {
    @Bean
    ExecutorService paymentExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
