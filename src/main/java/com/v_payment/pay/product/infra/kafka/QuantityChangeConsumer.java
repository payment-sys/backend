package com.v_payment.pay.product.infra.kafka;

import com.v_payment.pay.global.meter.KafkaMetrics;
import com.v_payment.pay.product.config.QuantityChangeConsumerProperties;
import com.v_payment.pay.product.infra.kafka.dto.QuantityChangeMessage;
import com.v_payment.pay.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.kafka.listener.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class QuantityChangeConsumer {
    private final ObjectMapper objectMapper;
    private final ProductService productService;
    private final QuantityChangeConsumerProperties properties;
    private final KafkaMetrics kafkaMetrics;

    @KafkaListener(
            topics = "${product.quantity-change-consumer.topic}",
            groupId = "${product.quantity-change-consumer.group-id}"
    )
    public void listen(String message) {
        kafkaMetrics.recordConsumerProcess(properties.topic(), "product-quantity-change-consumer", () -> {
            List<QuantityChangeMessage> quantityChangeMessages = parseMessage(message);
            productService.changeQuantityBatch(quantityChangeMessages);
        });
    }

    private List<QuantityChangeMessage> parseMessage(String message) {
        return List.of(objectMapper.readValue(message, QuantityChangeMessage.class));
    }
}
