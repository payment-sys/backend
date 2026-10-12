package com.v_payment.pay.product.infra.kafka.dto;

import com.v_payment.pay.product.domain.entity.ChangeStatus;

public record QuantityChangeResultMessage(
        String orderCode,
        Long productId,
        String productName,
        Long unitPrice,
        Integer changeCount,
        ChangeStatus changeStatus,
        Integer productsCount
) {
    public static QuantityChangeResultMessage of(
            String orderCode,
            Long productId,
            String productName,
            Long unitPrice,
            Integer changeCount,
            ChangeStatus changeStatus,
            Integer productsCount
    ) {
        return new QuantityChangeResultMessage(orderCode, productId, productName, unitPrice, changeCount, changeStatus, productsCount);
    }
}
