package com.v_payment.pay.payment.controller.dto.req;

import com.v_payment.pay.payment.entity.PaymentMethod;
import com.v_payment.pay.payment.entity.Provider;

public record ApprovalReq(
        PaymentMethod method,
        String orderId,
        String paymentKey,
        Long requestedAmount,
        Provider provider
) {
}
