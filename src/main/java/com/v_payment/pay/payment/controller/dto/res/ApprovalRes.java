package com.v_payment.pay.payment.controller.dto.res;

import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.Result;
import com.v_payment.pay.payment.infra.SuccessResult;

import java.time.LocalDateTime;

public record ApprovalRes(
        String orderCode,
        PaymentStatus status,
        Long totalAmount,
        LocalDateTime approvedAt,
        String receiptUrl
) {
    public static ApprovalRes from(Result result) {
        if (result instanceof SuccessResult successResult) {
            return from(successResult);
        }
        if (result instanceof FailedResult failedResult) {
            return from(failedResult);
        }
        throw new IllegalArgumentException("Unsupported approval result type: " + result.getClass().getName());
    }

    public static ApprovalRes from(SuccessResult successResult) {
        return new ApprovalRes(
                successResult.orderCode(),
                PaymentStatus.APPROVED,
                successResult.totalAmount(),
                successResult.approvedAt(),
                successResult.receipt().url()
        );
    }

    public static ApprovalRes from(FailedResult failedResult) {
        return new ApprovalRes(
                failedResult.orderCode(),
                PaymentStatus.REJECTED,
                null,
                null,
                null
        );
    }

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
