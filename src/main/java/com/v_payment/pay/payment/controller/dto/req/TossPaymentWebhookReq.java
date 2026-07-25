package com.v_payment.pay.payment.controller.dto.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentWebhookReq(
        String eventType,
        LocalDateTime createdAt,
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            @JsonAlias("orderId")
            String orderCode,
            String paymentKey,
            String status,
            Long totalAmount,
            OffsetDateTime approvedAt,
            Receipt receipt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Receipt(
            String url
    ) {
    }
}
