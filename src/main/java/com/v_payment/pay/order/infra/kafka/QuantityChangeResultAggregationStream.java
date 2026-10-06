package com.v_payment.pay.order.infra.kafka;

import com.v_payment.pay.order.config.QuantityChangeResultAggregationProperties;
import com.v_payment.pay.order.infra.kafka.dto.QuantityChangeResultMessage;
import com.v_payment.pay.order.infra.kafka.dto.QuantityChangeSummaryMessage;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
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
        KStream<String, QuantityChangeSummaryMessage> summaryMessages = builder
                .stream(properties.inputTopic(), Consumed.with(Serdes.String(), resultMessageSerde))
                .selectKey((key, message) -> message.orderCode())
                .groupByKey(Grouped.with(Serdes.String(), resultMessageSerde))
                .aggregate(
                        QuantityChangeResultAggregationState::new,
                        (orderCode, message, state) -> state.add(message),
                        Materialized.<String, QuantityChangeResultAggregationState, KeyValueStore<Bytes, byte[]>>as(STORE_NAME)
                                .withKeySerde(Serdes.String())
                                .withValueSerde(stateSerde)
                )
                .toStream()
                .filter((orderCode, state) -> state.isCompleted())
                .mapValues(state -> new QuantityChangeSummaryMessage(state.orderCode(), state.summaryStatus()));

        summaryMessages.to(properties.outputTopic(), Produced.with(Serdes.String(), summaryMessageSerde));
        return summaryMessages;
    }
}
