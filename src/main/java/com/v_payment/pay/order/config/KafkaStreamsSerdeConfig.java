package com.v_payment.pay.order.config;

import com.v_payment.pay.order.infrastructure.kafka.QuantityChangeResultAggregationState;
import com.v_payment.pay.order.infrastructure.kafka.dto.QuantityChangeResultMessage;
import com.v_payment.pay.order.infrastructure.kafka.dto.QuantityChangeSummaryMessage;
import org.apache.kafka.common.serialization.Serde;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;

@Configuration
public class KafkaStreamsSerdeConfig {
    @Bean
    public Serde<QuantityChangeResultMessage> quantityChangeResultMessageSerde() {
        return jsonSerde(QuantityChangeResultMessage.class);
    }

    @Bean
    public Serde<QuantityChangeResultAggregationState> quantityChangeResultAggregationStateSerde() {
        return jsonSerde(QuantityChangeResultAggregationState.class);
    }

    @Bean
    public Serde<QuantityChangeSummaryMessage> orderQuantityChangeSummaryMessageSerde() {
        return jsonSerde(QuantityChangeSummaryMessage.class);
    }

    private <T> Serde<T> jsonSerde(Class<T> type) {
        JacksonJsonSerde<T> serde = new JacksonJsonSerde<>(type);
        serde.deserializer().addTrustedPackages("com.v_payment.pay");
        serde.deserializer().ignoreTypeHeaders();
        serde.serializer().setAddTypeInfo(false);
        return serde;
    }
}
