package com.v_payment.pay.order.domain;

import com.v_payment.pay.payment.domain.entity.PaymentMethod;

public record OrderPaymentCreateSource(
        String orderCode,
        Long amount,
        PaymentMethod paymentMethod
) {
}
