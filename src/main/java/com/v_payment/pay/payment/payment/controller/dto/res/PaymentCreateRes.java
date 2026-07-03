package com.v_payment.pay.payment.payment.controller.dto.res;

import com.v_payment.pay.payment.payment.entity.Payment;

import java.math.BigDecimal;

public record PaymentCreateRes(
        String orderId,
        Long amount
) {
    public static PaymentCreateRes from(final Payment savedPayment) {
        return new PaymentCreateRes(savedPayment.getOrderId(), savedPayment.getRequestedAmount());
    }
}
