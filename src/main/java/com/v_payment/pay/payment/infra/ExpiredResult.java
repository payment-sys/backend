package com.v_payment.pay.payment.infra;

public record ExpiredResult(
        String orderCode,
        PaymentError paymentError,
        String message
) implements Result {
}
