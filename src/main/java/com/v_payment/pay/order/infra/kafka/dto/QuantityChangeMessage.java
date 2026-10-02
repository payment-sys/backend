package com.v_payment.pay.order.infra.kafka.dto;

public record QuantityChangeMessage(
        String orderCode,
        Long productId,
        Integer changeCount
) {
}
