package com.v_payment.pay.payment.infra;

import com.v_payment.pay.payment.entity.Payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FailedResult(
        String orderId,
        PaymentError paymentError,
        String message
) implements Result {
}
