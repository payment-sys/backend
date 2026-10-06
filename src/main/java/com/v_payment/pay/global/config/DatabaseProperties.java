package com.v_payment.pay.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.datasource")
public record DatabaseProperties(
        DataSourceProperties orderPayment,
        DataSourceProperties product
) {
    public record DataSourceProperties(
            String driverClassName,
            String url,
            String username,
            String password,
            HikariProperties hikari
    ) {
    }

    public record HikariProperties(
            Boolean autoCommit,
            Integer maximumPoolSize,
            Integer minimumIdle,
            Long connectionTimeout,
            Long validationTimeout,
            Long idleTimeout,
            Long maxLifetime,
            Long keepaliveTime,
            Boolean registerMbeans
    ) {
    }
}
