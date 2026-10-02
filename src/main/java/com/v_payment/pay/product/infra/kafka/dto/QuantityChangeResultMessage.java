package com.v_payment.pay.product.infra.kafka.dto;

import com.v_payment.pay.product.domain.entity.ChangeStatus;

public record QuantityChangeResultMessage(
        String orderCode,
        Long productId,
        ChangeStatus changeStatus
) {
    public static QuantityChangeResultMessage of(String orderCode, Long productId, ChangeStatus changeStatus) {
        return new QuantityChangeResultMessage(orderCode, productId, changeStatus);
    }
}
