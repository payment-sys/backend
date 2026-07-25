package com.v_payment.pay.payment.infra;

public record UnknownResult(
        String orderCode,
        PaymentError paymentError,
        String message
) implements Result {
}
