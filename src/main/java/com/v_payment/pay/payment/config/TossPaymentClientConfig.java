package com.v_payment.pay.payment.config;

import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class TossPaymentClientConfig {
    private final TossPaymentProperties tossPaymentProperties;
    private final RestClientBuilderConfigurer restClientBuilderConfigurer;

    @Bean
    public RestClient tossPaymentClient(
            @Qualifier("apacheRequestFactory")
            ClientHttpRequestFactory requestFactory
    ) {
        return restClientBuilderConfigurer.configure(RestClient.builder())
                .requestFactory(requestFactory)
                .build();
    }

    @Bean("jdkRequestFactory")
    public ClientHttpRequestFactory jdkRequestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(tossPaymentProperties.timeout()))
                .build();

        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(httpClient);

        factory.setReadTimeout(
                Duration.ofSeconds(tossPaymentProperties.timeout())
        );

        return factory;
    }

    @Bean("apacheRequestFactory")
    public ClientHttpRequestFactory apacheRequestFactory() {
        CloseableHttpClient httpClient = HttpClients.custom()
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        Duration timeout =
                Duration.ofSeconds(tossPaymentProperties.timeout());

        factory.setConnectionRequestTimeout(timeout);
        factory.setReadTimeout(timeout);

        return factory;
    }
}
