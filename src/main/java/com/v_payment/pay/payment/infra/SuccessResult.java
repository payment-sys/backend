package com.v_payment.pay.payment.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SuccessResult(
        String orderId,
        String paymentKey,
        String status,
        Long totalAmount,
        OffsetDateTime approvedAt,
        Receipt receipt
) implements Result {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Receipt(
            String url
    ){}
}
