package com.v_payment.pay.payment.controller.dto.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.v_payment.pay.payment.domain.entity.PaymentMethod;
import com.v_payment.pay.payment.domain.entity.Provider;


public record ApprovalReq(
        PaymentMethod method,
        @JsonAlias({"orderId", "idempotencyKey"})
        String orderCode,
        String paymentKey,
        @JsonAlias("amount")
        Long requestedAmount,
        Provider provider
) {
}
