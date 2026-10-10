package com.v_payment.pay.order.infrastructure.kafka.dto;

import com.v_payment.pay.order.domain.order.OrderItemInfo;

import java.util.List;

public record QuantityChangeSummaryMessage(
        String orderCode,
        QuantityChangeSummaryStatus status,
        List<OrderItemInfo> orderItems
) {
    public QuantityChangeSummaryMessage {
        orderItems = orderItems == null ? List.of() : List.copyOf(orderItems);
    }
}
