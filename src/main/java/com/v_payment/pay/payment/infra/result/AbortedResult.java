package com.v_payment.pay.payment.infra.result;

import com.v_payment.pay.payment.infra.PaymentError;

public record AbortedResult(
        String orderCode,
        PaymentError paymentError,
        String message
) implements Result {
}
