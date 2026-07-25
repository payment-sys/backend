package com.v_payment.pay.payment.service;

import com.v_payment.pay.global.exception.BusinessException;
import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.res.ApprovalRes;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.Result;
import com.v_payment.pay.payment.infra.SuccessResult;
import com.v_payment.pay.payment.infra.TossPayment;
import com.v_payment.pay.payment.repository.PaymentRepository;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.v_payment.pay.payment.exception.PaymentException.PAYMENT_INVALID;
import static com.v_payment.pay.payment.exception.PaymentException.PAYMENT_NOT_FOUND;
import static com.v_payment.pay.payment.exception.PaymentException.UNKNOWN_ERROR;

@Slf4j(topic = "API_LOGGER")
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentPayload validateApprovalReq(ApprovalReq approvalReq) {
        int updatedRows = paymentRepository.markInProgress(
                approvalReq.orderCode(),
                approvalReq.paymentKey(),
                approvalReq.requestedAmount(),
                approvalReq.provider(),
                approvalReq.method(),
                PaymentStatus.READY,
                PaymentStatus.IN_PROGRESS
        );
        validatePaymentUpdatedRows(updatedRows);

        return PaymentPayload.create(approvalReq.orderCode(), approvalReq.paymentKey(), approvalReq.requestedAmount());
    }

    @Transactional
    public ApprovalRes finalizePaymentPayload(Result approveResult) {
        if (approveResult instanceof SuccessResult successResult) {
            return applySuccessResult(successResult);
        }
        if (approveResult instanceof FailedResult failedResult) {
            return applyFailedResult(failedResult);
        }
        throw new BusinessException(UNKNOWN_ERROR);
    }

    private ApprovalRes applySuccessResult(SuccessResult successResult) {
        int updatedRows = paymentRepository.markDone(
                successResult.orderCode(),
                PaymentStatus.IN_PROGRESS,
                PaymentStatus.DONE,
                successResult.totalAmount(),
                successResult.approvedAt(),
                successResult.receipt().url()
        );
        validatePaymentUpdatedRows(updatedRows);
        return ApprovalRes.from(successResult);
    }

    private ApprovalRes applyFailedResult(FailedResult failedResult) {
        int updatedRows = paymentRepository.markAborted(
                failedResult.orderCode(),
                PaymentStatus.IN_PROGRESS,
                PaymentStatus.ABORTED,
                failedResult.message()
        );
        validatePaymentUpdatedRows(updatedRows);
        return ApprovalRes.from(failedResult);
    }

    private void validatePaymentUpdatedRows(int updatedRows) {
        if (updatedRows != 1) throw new BusinessException(PAYMENT_NOT_FOUND);
    }
}
