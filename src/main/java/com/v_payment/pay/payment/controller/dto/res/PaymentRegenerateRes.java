package com.v_payment.pay.payment.controller.dto.res;

import com.v_payment.pay.payment.domain.entity.PaymentStatus;

public record PaymentRegenerateRes(
        String orderCode,
        String idempotencyKey,
        PaymentStatus status
) {
    public static PaymentRegenerateRes ready(String orderCode, String idempotencyKey) {
        return new PaymentRegenerateRes(orderCode, idempotencyKey, PaymentStatus.READY);
    }
}
