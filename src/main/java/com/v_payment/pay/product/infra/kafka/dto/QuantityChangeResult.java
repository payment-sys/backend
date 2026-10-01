package com.v_payment.pay.product.infra.kafka.dto;

import com.v_payment.pay.product.domain.entity.ChangeStatus;

public record QuantityChangeResult(
        String orderCode,
        Long productId,
        ChangeStatus changeStatus
) {
    public static QuantityChangeResult of(String orderCode, Long productId, ChangeStatus changeStatus) {
        return new QuantityChangeResult(orderCode, productId, changeStatus);
    }
}
