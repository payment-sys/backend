package com.v_payment.pay.payment.entity;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor
@Builder(toBuilder = true)
public class PaymentPayload {
    private String orderId;

    private String paymentKey;

    private Long requestedAmount;

    public PaymentPayload(String orderId, String paymentKey, Long requestedAmount) {
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.requestedAmount = requestedAmount;
    }
}
