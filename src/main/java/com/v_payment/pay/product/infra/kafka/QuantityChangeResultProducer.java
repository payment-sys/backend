package com.v_payment.pay.product.infra.kafka;

import com.v_payment.pay.product.config.QuantityChangeResultProducerProperties;
import com.v_payment.pay.product.infra.kafka.dto.QuantityChangeResult;
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

    public CompletableFuture<?> send(QuantityChangeResult quantityChangeResult) {
        String topic = quantityChangeResultProducerProperties.topic();
        String key = quantityChangeResult.orderCode();
        String message = objectMapper.writeValueAsString(quantityChangeResult);
        return kafkaTemplate.send(topic, key, message);
    }
}
