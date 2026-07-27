package com.v_payment.pay.payment.infra.toss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.v_payment.pay.payment.infra.PaymentConfirmRes;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentConfirmSuccessRes(
        @JsonProperty("orderId")
        String orderCode,
        String paymentKey,
        String status,
        Long totalAmount,
        OffsetDateTime approvedAt,
        Receipt receipt
) implements PaymentConfirmRes {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Receipt(
            String url
    ) {
    }
}
