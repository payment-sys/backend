package com.v_payment.pay.order.infrastructure.kafka;

import com.v_payment.pay.order.config.QuantityChangeResultAggregationProperties;
import com.v_payment.pay.order.infrastructure.kafka.dto.QuantityChangeResultMessage;
import com.v_payment.pay.order.infrastructure.kafka.dto.QuantityChangeSummaryMessage;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.stereotype.Component;

@EnableKafkaStreams
@Component
@RequiredArgsConstructor
public class QuantityChangeResultAggregationStream {
    private static final String STORE_NAME = "order-quantity-change-result-store";

    private final QuantityChangeResultAggregationProperties properties;
    private final Serde<QuantityChangeResultMessage> resultMessageSerde;
    private final Serde<QuantityChangeResultAggregationState> stateSerde;
    private final Serde<QuantityChangeSummaryMessage> summaryMessageSerde;

    @Bean
    public KStream<String, QuantityChangeSummaryMessage> quantityChangeResultAggregation(StreamsBuilder builder) {
        StoreBuilder<KeyValueStore<String, QuantityChangeResultAggregationState>> storeBuilder =
                Stores.keyValueStoreBuilder(
                        Stores.persistentKeyValueStore(STORE_NAME),
                        Serdes.String(),
                        stateSerde
                );

        builder.addStateStore(storeBuilder);

        KStream<String, QuantityChangeSummaryMessage> summaries = builder
                .stream(properties.inputTopic(), Consumed.with(Serdes.String(), resultMessageSerde))
                .process(
                        () -> new QuantityChangeResultAggregationProcessor(STORE_NAME),
                        Named.as("quantity-change-result-aggregation-processor"),
                        STORE_NAME
                );

        summaries.to(properties.outputTopic(), Produced.with(Serdes.String(), summaryMessageSerde));
        return summaries;
    }
}
