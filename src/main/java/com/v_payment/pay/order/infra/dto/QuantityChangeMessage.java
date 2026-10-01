package com.v_payment.pay.order.infra.dto;

public record QuantityChangeMessage(
        String orderCode,
        Long productId,
        Integer changeCount
) {
}
