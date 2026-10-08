package com.v_payment.pay.order.domain;

import com.v_payment.pay.order.infrastructure.kafka.dto.QuantityChangeSummaryMessage;
import com.v_payment.pay.order.infrastructure.kafka.dto.QuantityChangeSummaryStatus;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class QuantityChangeSummaries {
    private final List<QuantityChangeSummaryMessage> messages;
    private final List<QuantityChangeSummaryMessage> failedMessages;
    private final List<QuantityChangeSummaryMessage> successMessages;
    private final Set<String> failedOrderCodes;
    private final Set<String> successOrderCodes;

    private QuantityChangeSummaries(List<QuantityChangeSummaryMessage> messages) {
        this.messages = validateMessages(messages);
        this.failedMessages = extractMessagesByStatus(this.messages, QuantityChangeSummaryStatus.FAILED);
        this.successMessages = extractSuccessMessagesExcludingFailedOrders(this.messages, this.failedMessages);

        this.failedOrderCodes = extractOrderCodes(this.failedMessages);
        this.successOrderCodes = extractOrderCodes(this.successMessages);
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public boolean hasFailedOrders() {
        return !failedMessages.isEmpty();
    }

    public boolean hasSuccessOrders() {
        return !successMessages.isEmpty();
    }

    public List<QuantityChangeSummaryMessage> getFailedMessages() {
        return failedMessages;
    }

    public List<QuantityChangeSummaryMessage> getSuccessMessages() {
        return successMessages;
    }

    public Set<String> getFailedOrderCodes() {
        return failedOrderCodes;
    }

    public Set<String> getSuccessOrderCodes() {
        return successOrderCodes;
    }

    public List<String> getOrderCodes() {
        return messages.stream()
                .map(QuantityChangeSummaryMessage::orderCode)
                .toList();
    }

    public List<QuantityChangeSummaryMessage> getMessages() {
        return messages;
    }

    public static QuantityChangeSummaries create(List<QuantityChangeSummaryMessage> messages) {
        return new QuantityChangeSummaries(messages);
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
        if (message.successQuantities() == null) {
            throw new IllegalArgumentException("successQuantities is required.");
        }
        if (message.failedQuantities() == null) {
            throw new IllegalArgumentException("failedQuantities is required.");
        }
    }

    private List<QuantityChangeSummaryMessage> extractMessagesByStatus(
            List<QuantityChangeSummaryMessage> messages,
            QuantityChangeSummaryStatus status
    ) {
        return messages.stream()
                .filter(message -> message.status() == status)
                .toList();
    }

    private List<QuantityChangeSummaryMessage> extractSuccessMessagesExcludingFailedOrders(
            List<QuantityChangeSummaryMessage> messages,
            List<QuantityChangeSummaryMessage> failedMessages
    ) {
        Set<String> failedOrderCodes = extractOrderCodes(failedMessages);

        return messages.stream()
                .filter(message -> message.status() == QuantityChangeSummaryStatus.SUCCESS)
                .filter(message -> !failedOrderCodes.contains(message.orderCode()))
                .toList();
    }

    private Set<String> extractOrderCodes(List<QuantityChangeSummaryMessage> messages) {
        return messages.stream()
                .map(QuantityChangeSummaryMessage::orderCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
