package com.v_payment.pay.payment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PaymentOutboxSchedulerProperties.class)
public class PaymentOutboxConfig {
}
