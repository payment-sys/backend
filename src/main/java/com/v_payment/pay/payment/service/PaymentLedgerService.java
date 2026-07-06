package com.v_payment.pay.payment.service;

import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.infra.PaymentError;
import com.v_payment.pay.payment.repository.PaymentLedgerRepository;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.entity.Provider;
import com.v_payment.pay.payment.infra.SuccessResult;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentLedgerService {
    private final Clock clock;
    private final PaymentLedgerRepository paymentLedgerRepository;

    public void insertPaymentLedgerPENDING(Payment payment) {
        insertPaymentLedger(payment, null, PaymentStatus.PENDING, null, null,
                null);
    }

    public void insertPaymentLedgerAPPROVING(Payment payment) {
        insertPaymentLedger(payment, PaymentStatus.PENDING, PaymentStatus.APPROVING, null, null,
                null);
    }

    public void insertPaymentLedgerAPPROVING(ApprovalReq approvalReq) {
        paymentLedgerRepository.insertPaymentLedger(
                approvalReq.orderId(),
                approvalReq.paymentKey(),
                approvalReq.provider().name(),
                PaymentStatus.PENDING.name(),
                PaymentStatus.APPROVING.name(),
                null,
                null,
                approvalReq.requestedAmount(),
                null,
                LocalDateTime.now(clock)
        );
    }

    public void insertPaymentLedgerAPPROVED(PaymentPayload paymentPayload, SuccessResult successResult) {
        paymentLedgerRepository.insertPaymentLedger(
                paymentPayload.getOrderId(),
                paymentPayload.getPaymentKey(),
                Provider.TOSS.name(),
                PaymentStatus.APPROVING.name(),
                PaymentStatus.APPROVED.name(),
                null,
                null,
                paymentPayload.getAmount(),
                successResult.totalAmount(),
                LocalDateTime.now(clock)
        );
    }

    public void insertPaymentLedgerREJECTED(Payment payment, FailedResult failedResult) {
        insertPaymentLedger(payment, PaymentStatus.APPROVING, PaymentStatus.REJECTED, failedResult.paymentError().name(),
                failedResult.message(), null);
    }

    public void insertPaymentLedgerREJECTED(PaymentPayload paymentPayload, FailedResult failedResult) {
        paymentLedgerRepository.insertPaymentLedger(
                paymentPayload.getOrderId(),
                paymentPayload.getPaymentKey(),
                Provider.TOSS.name(),
                PaymentStatus.APPROVING.name(),
                PaymentStatus.REJECTED.name(),
                failedResult.paymentError().name(),
                failedResult.message(),
                paymentPayload.getAmount(),
                null,
                LocalDateTime.now(clock)
        );
    }

    private void insertPaymentLedger(Payment payment,
                                     PaymentStatus fromStatus,
                                     PaymentStatus toStatus,
                                     String failedCode,
                                     String failedMessage,
                                     Long approvedAmount) {
        paymentLedgerRepository.insertPaymentLedger(
                payment.getOrderId(),
                payment.getPaymentKey(),
                payment.getProvider().name(),
                fromStatus == null ? null : fromStatus.name(),
                toStatus.name(),
                failedCode,
                failedMessage,
                payment.getRequestedAmount(),
                approvedAmount,
                LocalDateTime.now(clock)
        );
    }
}
