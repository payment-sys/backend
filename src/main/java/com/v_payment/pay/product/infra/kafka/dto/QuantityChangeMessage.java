package com.v_payment.pay.product.infra.kafka.dto;

public record QuantityChangeMessage(
        String orderCode,
        Long productId,
        Integer changeCount
) {
}
