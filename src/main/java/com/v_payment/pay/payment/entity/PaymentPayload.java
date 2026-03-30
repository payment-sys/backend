package com.v_payment.pay.payment.entity;

import lombok.*;

@Getter
public class PaymentPayload {
    private final String orderId;
    private final String paymentKey;
    private final Long amount;

    @Builder
    public PaymentPayload(String orderId, String paymentKey, Long amount) {
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.amount = amount;
    }

    public static PaymentPayload create(String orderId, String paymentKey, Long requestedAmount) {
        return PaymentPayload.builder()
                .orderId(orderId)
                .paymentKey(paymentKey)
                .amount(requestedAmount)
                .build();
    }
}
