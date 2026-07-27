package com.v_payment.pay.payment.infra.result;

import com.v_payment.pay.payment.infra.PaymentError;

public record UnknownResult(
        String orderCode,
        PaymentError paymentError,
        String message
) implements Result {
}
