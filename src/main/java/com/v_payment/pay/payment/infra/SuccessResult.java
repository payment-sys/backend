package com.v_payment.pay.payment.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SuccessResult(
        String orderId,
        String paymentKey,
        String status,
        Long totalAmount,
        LocalDateTime approvedAt,
        Receipt receipt
) implements Result {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Receipt(
            String url
    ){}
}
