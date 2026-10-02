package com.v_payment.pay.order.domain;

import com.v_payment.pay.order.infra.kafka.dto.QuantityChangeResultMessage;
import com.v_payment.pay.product.domain.entity.ChangeStatus;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class QuantityChangeResultPlan {
    private final List<QuantityChangeResultMessage> messages;
    private final Set<String> orderCodes;
    private final Set<String> failedOrderCodes;

    private QuantityChangeResultPlan(List<QuantityChangeResultMessage> messages) {
        this.messages = validateMessages(messages);
        this.orderCodes = extractOrderCodes(this.messages);
        this.failedOrderCodes = extractFailedOrderCodes(this.messages);
    }

    public static QuantityChangeResultPlan create(List<QuantityChangeResultMessage> messages) {
        return new QuantityChangeResultPlan(messages);
    }

    public List<QuantityChangeResultMessage> getMessages() {
        return messages;
    }

    public Set<String> getOrderCodes() {
        return orderCodes;
    }

    public Set<String> getFailedOrderCodes() {
        return failedOrderCodes;
    }

    public boolean hasFailedOrders() {
        return !failedOrderCodes.isEmpty();
    }

    public List<String> getCompletedOrderCodes(List<OrderPaymentCreateSource> paymentCreateSources) {
        if (paymentCreateSources == null || paymentCreateSources.isEmpty()) {
            return List.of();
        }

        return paymentCreateSources.stream()
                .map(OrderPaymentCreateSource::orderCode)
                .toList();
    }

    private List<QuantityChangeResultMessage> validateMessages(List<QuantityChangeResultMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("quantity change result messages are required.");
        }

        messages.forEach(this::validateMessage);
        return List.copyOf(messages);
    }

    private void validateMessage(QuantityChangeResultMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("quantity change result message is required.");
        }
        if (message.orderCode() == null || message.orderCode().isBlank()) {
            throw new IllegalArgumentException("orderCode is required.");
        }
        if (message.productId() == null) {
            throw new IllegalArgumentException("productId is required.");
        }
        if (message.changeStatus() == null) {
            throw new IllegalArgumentException("changeStatus is required.");
        }
    }

    private Set<String> extractOrderCodes(List<QuantityChangeResultMessage> messages) {
        return messages.stream()
                .map(QuantityChangeResultMessage::orderCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> extractFailedOrderCodes(List<QuantityChangeResultMessage> messages) {
        return messages.stream()
                .filter(message -> message.changeStatus() == ChangeStatus.FAILED)
                .map(QuantityChangeResultMessage::orderCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

}
