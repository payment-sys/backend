package com.v_payment.pay.payment.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PaymentPayload {
    @JsonIgnore
    private final String orderCode;

    @JsonProperty("orderId")
    private final String idempotencyKey;

    private final String paymentKey;
    private final Long amount;

    @Builder
    public PaymentPayload(String orderCode, String idempotencyKey, String paymentKey, Long amount) {
        this.orderCode = orderCode;
        this.idempotencyKey = idempotencyKey;
        this.paymentKey = paymentKey;
        this.amount = amount;
    }

    public static PaymentPayload create(String orderCode, String idempotencyKey, String paymentKey, Long amount) {
        return PaymentPayload.builder()
                .orderCode(orderCode)
                .idempotencyKey(idempotencyKey)
                .paymentKey(paymentKey)
                .amount(amount)
                .build();
    }
}
