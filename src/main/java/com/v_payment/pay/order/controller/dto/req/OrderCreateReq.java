package com.v_payment.pay.order.controller.dto.req;

import com.v_payment.pay.payment.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderCreateReq(
        @NotNull
        PaymentMethod paymentMethod,

        @NotEmpty
        List<@Valid OrderItemCreateReq> items
) {
}
