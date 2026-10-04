package com.v_payment.pay.product.infra.kafka;

import com.v_payment.pay.global.meter.KafkaMetrics;
import com.v_payment.pay.product.config.QuantityChangeResultProducerProperties;
import com.v_payment.pay.product.infra.kafka.dto.QuantityChangeResultMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class QuantityChangeResultProducer {
    private final QuantityChangeResultProducerProperties quantityChangeResultProducerProperties;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaMetrics kafkaMetrics;

    public CompletableFuture<?> send(QuantityChangeResultMessage quantityChangeResultMessage) {
        String topic = quantityChangeResultProducerProperties.topic();
        String key = quantityChangeResultMessage.orderCode();
        String message = objectMapper.writeValueAsString(quantityChangeResultMessage);
        return kafkaMetrics.recordProducerSend(topic, kafkaTemplate.send(topic, key, message));
    }
}
