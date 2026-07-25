package com.v_payment.pay.payment.infra;

public record AbortedResult(
        String orderCode,
        PaymentError paymentError,
        String message
) implements Result {
}
