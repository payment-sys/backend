package com.v_payment.pay.payment.controller.dto.req;

import com.v_payment.pay.payment.entity.PaymentMethod;
import com.v_payment.pay.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.entity.Provider;

public record PaymentCreateReq(
        Long requestedAmount,
        String name,
        PaymentMethod paymentMethod,
        Provider provider
) {
}
