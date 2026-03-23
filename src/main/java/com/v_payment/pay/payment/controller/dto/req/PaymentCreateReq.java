package com.v_payment.pay.payment.controller.dto.req;

import com.v_payment.pay.payment.entity.PaymentMethod;

public record PaymentCreateReq(
        Long requestedAmount,
        PaymentMethod paymentMethod
) {
}
