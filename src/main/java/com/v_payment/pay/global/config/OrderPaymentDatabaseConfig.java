package com.v_payment.pay.global.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = {
                "com.v_payment.pay.order.infrastructure.persistence.repository",
                "com.v_payment.pay.payment.repository"
        },
        entityManagerFactoryRef = "orderPaymentEntityManagerFactory",
        transactionManagerRef = "orderPaymentTransactionManager"
)
public class OrderPaymentDatabaseConfig {

    @Bean(name = {"dataSource", "orderPaymentDataSource"})
    @Primary
    public DataSource orderPaymentDataSource(DatabaseProperties properties) {
        return createDataSource("order-payment", properties.orderPayment());
    }

    @Bean(name = {"jdbcTemplate", "orderPaymentJdbcTemplate"})
    @Primary
    public JdbcTemplate orderPaymentJdbcTemplate(
            @Qualifier("orderPaymentDataSource") DataSource dataSource
    ) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = {"entityManagerFactory", "orderPaymentEntityManagerFactory"})
    @Primary
    public LocalContainerEntityManagerFactoryBean orderPaymentEntityManagerFactory(
            @Qualifier("orderPaymentDataSource") DataSource dataSource,
            Environment environment
    ) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan(
                "com.v_payment.pay.order.domain",
                "com.v_payment.pay.payment.domain"
        );
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(jpaProperties(environment));
        return factory;
    }

    @Bean(name = {"transactionManager", "orderPaymentTransactionManager"})
    @Primary
    public PlatformTransactionManager orderPaymentTransactionManager(
            @Qualifier("orderPaymentEntityManagerFactory") EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    static DataSource createDataSource(String poolName, DatabaseProperties.DataSourceProperties properties) {
        if (properties == null) {
            throw new IllegalStateException("app.datasource." + poolName + " is required");
        }

        HikariConfig config = new HikariConfig();
        config.setPoolName(poolName);
        config.setDriverClassName(properties.driverClassName());
        config.setJdbcUrl(properties.url());
        config.setUsername(properties.username());
        config.setPassword(properties.password());

        DatabaseProperties.HikariProperties hikari = properties.hikari();
        if (hikari != null) {
            if (hikari.autoCommit() != null) config.setAutoCommit(hikari.autoCommit());
            if (hikari.maximumPoolSize() != null) config.setMaximumPoolSize(hikari.maximumPoolSize());
            if (hikari.minimumIdle() != null) config.setMinimumIdle(hikari.minimumIdle());
            if (hikari.connectionTimeout() != null) config.setConnectionTimeout(hikari.connectionTimeout());
            if (hikari.validationTimeout() != null) config.setValidationTimeout(hikari.validationTimeout());
            if (hikari.idleTimeout() != null) config.setIdleTimeout(hikari.idleTimeout());
            if (hikari.maxLifetime() != null) config.setMaxLifetime(hikari.maxLifetime());
            if (hikari.keepaliveTime() != null) config.setKeepaliveTime(hikari.keepaliveTime());
            if (hikari.registerMbeans() != null) config.setRegisterMbeans(hikari.registerMbeans());
        }

        return new HikariDataSource(config);
    }

    static Map<String, Object> jpaProperties(Environment environment) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(
                "hibernate.hbm2ddl.auto",
                environment.getProperty("spring.jpa.hibernate.ddl-auto", "none")
        );
        properties.put("hibernate.physical_naming_strategy", PhysicalNamingStrategySnakeCaseImpl.class.getName());
        properties.put("hibernate.type.json_format_mapper", "org.hibernate.type.format.jackson.JacksonJsonFormatMapper");
        properties.put("hibernate.connection.provider_disables_autocommit", "true");
        return properties;
    }
}
