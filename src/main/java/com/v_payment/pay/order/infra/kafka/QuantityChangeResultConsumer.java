package com.v_payment.pay.order.infra.kafka;

import com.v_payment.pay.global.meter.KafkaMetrics;
import com.v_payment.pay.order.config.QuantityChangeResultConsumerProperties;
import com.v_payment.pay.order.infra.kafka.dto.QuantityChangeResultMessage;
import com.v_payment.pay.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.kafka.listener.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class QuantityChangeResultConsumer {
    private final ObjectMapper objectMapper;
    private final OrderService orderService;
    private final QuantityChangeResultConsumerProperties properties;
    private final KafkaMetrics kafkaMetrics;

    @KafkaListener(
            topics = "${order.quantity-change-result-consumer.topic}",
            groupId = "${order.quantity-change-result-consumer.group-id}",
            containerFactory = "quantityChangeResultBatchKafkaListenerContainerFactory"
    )
    public void listen(List<String> message) {
        kafkaMetrics.recordConsumerProcess(properties.topic(), "order-quantity-change-result-consumer", () -> {
            List<QuantityChangeResultMessage> quantityChangeResultMessages = parseMessage(message);
            orderService.finalizeOrderBatch(quantityChangeResultMessages);
        });
    }

    private List<QuantityChangeResultMessage> parseMessage(List<String> messages) {
        return messages.stream().map(msg -> objectMapper.readValue(msg, QuantityChangeResultMessage.class))
                .toList();
    }
}
