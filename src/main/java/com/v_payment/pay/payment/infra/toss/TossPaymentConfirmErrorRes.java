package com.v_payment.pay.payment.infra.toss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.v_payment.pay.payment.infra.PaymentConfirmRes;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentConfirmErrorRes(
        Integer httpStatusCode,
        String code,
        String message
) implements PaymentConfirmRes {
}
