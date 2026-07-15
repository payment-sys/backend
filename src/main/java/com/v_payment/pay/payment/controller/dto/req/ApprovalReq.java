package com.v_payment.pay.payment.controller.dto.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.v_payment.pay.payment.entity.PaymentMethod;
import com.v_payment.pay.payment.entity.Provider;


public record ApprovalReq(
        PaymentMethod method,
        @JsonAlias("orderId")
        String orderCode,
        String paymentKey,
        Long requestedAmount,
        Provider provider
) {
}
