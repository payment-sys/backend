package com.v_payment.pay.payment.infra;

public record RetryableResult(
        String orderId,
        PaymentError paymentError,
        String message
) implements Result {
}
