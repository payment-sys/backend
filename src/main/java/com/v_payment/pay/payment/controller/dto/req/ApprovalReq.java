package com.v_payment.pay.payment.controller.dto.req;

public record ApprovalReq(
        String method,
        String orderId,
        String paymentKey,
        Long requestedAmount,
        String provider
) {
}
