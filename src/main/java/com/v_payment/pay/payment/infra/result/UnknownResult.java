package com.v_payment.pay.payment.infra.result;

import com.v_payment.pay.payment.infra.PaymentError;

public record UnknownResult(
        String orderCode,
        String idempotencyKey,
        PaymentError paymentError,
        String message
) implements Result {
    @Override
    public String getOrderCode() {
        return orderCode;
    }

    @Override
    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
