package com.v_payment.pay.payment.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PaymentPayload {
    @JsonProperty("orderId")
    private final String orderCode;
    private final String paymentKey;
    private final Long amount;

    @Builder
    public PaymentPayload(String orderCode, String paymentKey, Long amount) {
        this.orderCode = orderCode;
        this.paymentKey = paymentKey;
        this.amount = amount;
    }

    public static PaymentPayload create(String orderCode, String paymentKey, Long amount) {
        return PaymentPayload.builder()
                .orderCode(orderCode)
                .paymentKey(paymentKey)
                .amount(amount)
                .build();
    }
}
