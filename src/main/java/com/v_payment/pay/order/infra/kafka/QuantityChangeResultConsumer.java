package com.v_payment.pay.order.infra.kafka;

import com.v_payment.pay.order.infra.kafka.dto.QuantityChangeResultMessage;
import com.v_payment.pay.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
public class QuantityChangeResultConsumer {
    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    @KafkaListener(
            topics = "${product.kafka.consumers.quantity-change-result.topic}",
            groupId = "${product.kafka.consumers.quantity-change-result.group-id}",
            containerFactory = "quantityChangeResultBatchKafkaListenerContainerFactory"
    )
    public void listen(List<String> message) {
        List<QuantityChangeResultMessage> quantityChangeResultMessages = parseMessage(message);
        orderService.finalizeOrderBatch(quantityChangeResultMessages);
    }

    private List<QuantityChangeResultMessage> parseMessage(List<String> messages) {
        return messages.stream().map(msg -> objectMapper.readValue(msg, QuantityChangeResultMessage.class))
                .toList();
    }
}
