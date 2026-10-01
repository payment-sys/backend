package com.v_payment.pay.order.infra;

import com.v_payment.pay.order.config.QuantityChangeProducerProperties;
import com.v_payment.pay.order.infra.dto.QuantityChangeMessage;
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

    public CompletableFuture<?> send(QuantityChangeMessage quantityChangeMessage) {
        String topic = producerProperties.topic();
        String key = String.valueOf(quantityChangeMessage.productId());
        String message = objectMapper.writeValueAsString(quantityChangeMessage);

        return kafkaTemplate.send(topic, key, message);
    }
}
