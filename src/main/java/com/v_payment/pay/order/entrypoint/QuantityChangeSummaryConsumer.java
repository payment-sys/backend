package com.v_payment.pay.order.entrypoint;

import com.v_payment.pay.global.meter.KafkaMetrics;
import com.v_payment.pay.order.application.QuantityChangeSummaryUseCase;
import com.v_payment.pay.order.config.QuantityChangeSummaryConsumerProperties;
import com.v_payment.pay.order.infrastructure.kafka.dto.QuantityChangeSummaryMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.kafka.listener.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class QuantityChangeSummaryConsumer {
    private final ObjectMapper objectMapper;
    private final QuantityChangeSummaryUseCase quantityChangeSummaryUseCase;
    private final QuantityChangeSummaryConsumerProperties properties;
    private final KafkaMetrics kafkaMetrics;

    @KafkaListener(
            topics = "${order.quantity-change-summary-consumer.topic}",
            groupId = "${order.quantity-change-summary-consumer.group-id}",
            containerFactory = "quantityChangeSummaryBatchKafkaListenerContainerFactory"
    )
    public void listen(List<String> messages) {
        kafkaMetrics.recordConsumerProcess(properties.topic(), "order-quantity-change-summary-consumer", () -> {
            List<QuantityChangeSummaryMessage> quantityChangeSummaryMessages = parseMessages(messages);
            log.debug("메시지 소비중 count={}", quantityChangeSummaryMessages.size());
            quantityChangeSummaryUseCase.finalizeOrderSummaryBatch(quantityChangeSummaryMessages);
        });
    }

    private List<QuantityChangeSummaryMessage> parseMessages(List<String> messages) {
        return messages.stream().map(msg -> objectMapper.readValue(msg, QuantityChangeSummaryMessage.class))
                .toList();
    }
}
