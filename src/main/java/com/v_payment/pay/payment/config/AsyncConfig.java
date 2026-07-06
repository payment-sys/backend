package com.v_payment.pay.payment.config;

import com.v_payment.pay.payment.entity.Payment;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean(name = {"taskExecutor", "paymentSubmitExecutor"})
    public AsyncTaskExecutor virtualThreadTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("payment-submit-vt-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(300);
        return executor;
    }
}
