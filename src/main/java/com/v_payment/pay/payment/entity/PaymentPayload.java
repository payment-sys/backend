package com.v_payment.pay.payment.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.util.UUID;

@Getter
public class PaymentPayload {
    private final String orderId;
    private final String paymentKey;
    private final Long requestedAmount;

    @Builder
    public PaymentPayload(String orderId, String paymentKey, Long requestedAmount) {
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.requestedAmount = requestedAmount;
    }

    public static PaymentPayload create(String orderId, String paymentKey, Long requestedAmount) {
        return PaymentPayload.builder()
                .orderId(orderId)
                .paymentKey(paymentKey)
                .requestedAmount(requestedAmount)
                .build();
    }
}
