package com.v_payment.pay.order.infra.kafka.dto;

public record QuantityChangeMessage(
        String orderCode,
        Long productId,
        Integer changeCount,
        Integer productsCount,
        QuantityChangeType type
) {
    public QuantityChangeMessage {
        type = type == null ? QuantityChangeType.DECREASE : type;
    }
}
