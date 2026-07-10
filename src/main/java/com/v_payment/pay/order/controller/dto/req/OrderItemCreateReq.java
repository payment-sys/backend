package com.v_payment.pay.order.controller.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemCreateReq(
        @NotNull
        Long productId,

        @NotNull
        @Positive
        Integer quantity
) {
}
