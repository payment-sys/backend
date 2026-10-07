package com.v_payment.pay.order.domain;

import com.v_payment.pay.order.infra.kafka.dto.QuantityChangeSummaryMessage;
import com.v_payment.pay.order.infra.kafka.dto.QuantityChangeSummaryStatus;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class QuantityChangeSummaries {
    private final List<QuantityChangeSummaryMessage> messages;
    private final Set<String> failedOrderCodes;
    private final Set<String> successOrderCodes;

    private QuantityChangeSummaries(List<QuantityChangeSummaryMessage> messages) {
        this.messages = validateMessages(messages);
        this.failedOrderCodes = extractOrderCodesByStatus(this.messages, QuantityChangeSummaryStatus.FAILED);
        this.successOrderCodes = extractOrderCodesByStatus(this.messages, QuantityChangeSummaryStatus.SUCCESS);
        this.successOrderCodes.removeAll(failedOrderCodes);
    }

    public static QuantityChangeSummaries create(List<QuantityChangeSummaryMessage> messages) {
        return new QuantityChangeSummaries(messages);
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public boolean hasFailedOrders() {
        return !failedOrderCodes.isEmpty();
    }

    public boolean hasSuccessOrders() {
        return !successOrderCodes.isEmpty();
    }

    public Set<String> getFailedOrderCodes() {
        return failedOrderCodes;
    }

    public Set<String> getSuccessOrderCodes() {
        return successOrderCodes;
    }

    public List<QuantityChangeSummaryMessage> getMessages() {
        return messages;
    }

    private List<QuantityChangeSummaryMessage> validateMessages(List<QuantityChangeSummaryMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        messages.forEach(this::validateMessage);
        return List.copyOf(messages);
    }

    private void validateMessage(QuantityChangeSummaryMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("quantity change summary message is required.");
        }
        if (message.orderCode() == null || message.orderCode().isBlank()) {
            throw new IllegalArgumentException("orderCode is required.");
        }
        if (message.status() == null) {
            throw new IllegalArgumentException("status is required.");
        }
        if (message.successProductIds() == null) {
            throw new IllegalArgumentException("successProductIds is required.");
        }
        if (message.failedProductIds() == null) {
            throw new IllegalArgumentException("failedProductIds is required.");
        }
    }

    private Set<String> extractOrderCodesByStatus(
            List<QuantityChangeSummaryMessage> messages,
            QuantityChangeSummaryStatus status
    ) {
        return messages.stream()
                .filter(message -> message.status() == status)
                .map(QuantityChangeSummaryMessage::orderCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
