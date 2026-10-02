package com.v_payment.pay.order.infra.kafka.dto;

import com.v_payment.pay.product.domain.entity.ChangeStatus;

public record QuantityChangeResultMessage(
        String orderCode,
        Long productId,
        ChangeStatus changeStatus
) {
}
