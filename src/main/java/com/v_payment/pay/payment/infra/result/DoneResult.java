package com.v_payment.pay.payment.infra.result;

import java.time.LocalDateTime;

public record DoneResult(
        String orderCode,
        String idempotencyKey,
        String paymentKey,
        Long totalAmount,
        LocalDateTime approvedAt,
        Receipt receipt
) implements Result {
    @Override
    public String getOrderCode() {
        return orderCode;
    }

    @Override
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public record Receipt(
            String url
    ) {
    }
}
