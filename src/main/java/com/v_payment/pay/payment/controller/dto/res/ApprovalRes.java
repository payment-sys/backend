package com.v_payment.pay.payment.controller.dto.res;

import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record ApprovalRes(
    String orderId,
    PaymentStatus status,
    Long totalAmount,
    LocalDateTime approvedAt,
    String receiptUrl
) {
    public static ApprovalRes from(Payment approveResult) {
        return new ApprovalRes(
                approveResult.getOrderId(),
                approveResult.getPaymentStatus(),
                approveResult.getApprovedAmount(),
                approveResult.getApprovedAt(),
                approveResult.getReceiptUrl()
        );
    }
}
