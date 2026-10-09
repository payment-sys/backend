package com.v_payment.pay.order.infrastructure.kafka.dto;

import com.v_payment.pay.product.domain.entity.ChangeStatus;

public record QuantityChangeResultMessage(
        String orderCode,
        Long productId,
        Integer changeCount,
        ChangeStatus changeStatus,
        Integer productsCount
) {
}
