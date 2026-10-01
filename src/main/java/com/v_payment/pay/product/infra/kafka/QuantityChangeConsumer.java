package com.v_payment.pay.product.infra.kafka;

import com.v_payment.pay.product.infra.kafka.dto.QuantityChangeMessage;
import com.v_payment.pay.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
public class QuantityChangeConsumer {
    private final ObjectMapper objectMapper;
    private final ProductService productService;

    @KafkaListener(
            topics = "${product.kafka.consumers.quantity-change.topic}",
            groupId = "${product.kafka.consumers.quantity-change.group-id}"
    )
    public void listen(String message) {
        List<QuantityChangeMessage> quantityChangeMessages = parseMessage(message);
        productService.changeQuantityBatch(quantityChangeMessages);
    }

    private List<QuantityChangeMessage> parseMessage(String message) {
        return List.of(objectMapper.readValue(message, QuantityChangeMessage.class));
    }
}
