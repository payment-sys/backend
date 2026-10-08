package com.v_payment.pay.order.infra.kafka;

import com.v_payment.pay.global.meter.KafkaMetrics;
import com.v_payment.pay.order.config.QuantityChangeProducerProperties;
import com.v_payment.pay.order.infra.kafka.dto.QuantityChangeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class QuantityChangeProducer {
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final QuantityChangeProducerProperties producerProperties;
    private final KafkaMetrics kafkaMetrics;

    public CompletableFuture<?> send(QuantityChangeMessage quantityChangeMessage) {
        String topic = producerProperties.topic();
        String key = String.valueOf(quantityChangeMessage.productId());
        String message = objectMapper.writeValueAsString(quantityChangeMessage);

        return kafkaMetrics.recordProducerSend(topic, kafkaTemplate.send(topic, key, message));
    }
}
