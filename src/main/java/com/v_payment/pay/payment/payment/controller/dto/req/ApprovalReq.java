package com.v_payment.pay.payment.payment.controller.dto.req;

import com.v_payment.pay.payment.payment.entity.PaymentMethod;
import com.v_payment.pay.payment.payment.entity.Provider;

public record ApprovalReq(
        PaymentMethod method,
        String orderId,
        String paymentKey,
        Long requestedAmount,
        Provider provider
) {
}
