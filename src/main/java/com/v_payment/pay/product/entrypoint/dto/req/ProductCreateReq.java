package com.v_payment.pay.product.entrypoint.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductCreateReq(
        @NotBlank
        String name,

        @NotNull
        @Positive
        Long price,

        @NotNull
        @PositiveOrZero
        Integer stockQuantity
) {
}
