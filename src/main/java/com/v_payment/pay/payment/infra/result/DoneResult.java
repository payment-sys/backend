package com.v_payment.pay.payment.infra.result;

import java.time.LocalDateTime;

public record DoneResult(
        String orderCode,
        String paymentKey,
        Long totalAmount,
        LocalDateTime approvedAt,
        Receipt receipt
) implements Result {
    public record Receipt(
            String url
    ) {
    }
}
