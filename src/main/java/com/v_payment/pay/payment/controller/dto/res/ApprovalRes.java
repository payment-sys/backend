package com.v_payment.pay.payment.controller.dto.res;

import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.infra.AbortedResult;
import com.v_payment.pay.payment.infra.DoneResult;
import com.v_payment.pay.payment.infra.UnknownResult;

import java.time.LocalDateTime;

public record ApprovalRes(
        String orderCode,
        PaymentStatus status,
        Long totalAmount,
        LocalDateTime approvedAt,
        String receiptUrl
) {
    public static ApprovalRes from(DoneResult doneResult) {
        return new ApprovalRes(
                doneResult.orderCode(),
                PaymentStatus.DONE,
                doneResult.totalAmount(),
                doneResult.approvedAt(),
                doneResult.receipt().url()
        );
    }

    public static ApprovalRes from(AbortedResult abortedResult) {
        return new ApprovalRes(
                abortedResult.orderCode(),
                PaymentStatus.ABORTED,
                null,
                null,
                null
        );
    }

    public static ApprovalRes from(UnknownResult unknownResult) {
        return new ApprovalRes(
                unknownResult.orderCode(),
                PaymentStatus.UNKNOWN,
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
