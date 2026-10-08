package com.v_payment.pay.order.domain;

import com.v_payment.pay.payment.domain.entity.PaymentMethod;

public record PaymentCreateSource(
        String orderCode,
        Long amount,
        PaymentMethod paymentMethod
) {
    public static PaymentCreateSource create(String orderCode, Long amount, PaymentMethod paymentMethod) {
        return new PaymentCreateSource(orderCode, amount, paymentMethod);
    }
}
