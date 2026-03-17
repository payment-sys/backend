package com.v_payment.pay.payment.controller.dto.req;

public record PaymentCreateReq(
        Long amount,
        String name
) {
}
