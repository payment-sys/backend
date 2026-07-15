package com.v_payment.pay.payment.controller.dto.res;

import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record ApprovalRes(
        String orderCode,
        PaymentStatus status,
        Long totalAmount,
        LocalDateTime approvedAt,
        String receiptUrl
) {
    public static ApprovalRes from(Payment payment) {
        return new ApprovalRes(
                payment.getOrderCode(),
                payment.getPaymentStatus(),
                payment.getApprovedAmount(),
                payment.getApprovedAt(),
                payment.getReceiptUrl()
        );
    }
}
