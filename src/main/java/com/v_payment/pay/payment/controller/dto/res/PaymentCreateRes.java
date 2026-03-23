package com.v_payment.pay.payment.controller.dto.res;

import com.v_payment.pay.payment.entity.Payment;
import java.math.BigDecimal;

public record PaymentCreateRes(
        String orderId,
        Long requestedAmount
) {

    public static PaymentCreateRes from(Payment savedPayment) {
        return new PaymentCreateRes(
            savedPayment.getOrderId(),
            savedPayment.getRequestedAmount()
        );
    }
}
