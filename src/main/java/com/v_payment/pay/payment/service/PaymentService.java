package com.v_payment.pay.payment.service;

import com.v_payment.pay.global.exception.BusinessException;
import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
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
    private final TossPayment tossPayment;
    private final PaymentRepository paymentRepository;

    @Transactional
    @WithSpan("payment.service.validate_approval_request")
    public PaymentPayload validateApprovalReq(ApprovalReq approvalReq) {
        int updatedRows = paymentRepository.markApproving(
                approvalReq.orderCode(),
                approvalReq.paymentKey(),
                approvalReq.requestedAmount(),
                approvalReq.provider(),
                approvalReq.method(),
                PaymentStatus.PENDING,
                PaymentStatus.APPROVING
        );
        validateMarkApprovingUpdatedRows(updatedRows, approvalReq);

        return PaymentPayload.create(approvalReq.orderCode(), approvalReq.paymentKey(), approvalReq.requestedAmount());
    }

    @WithSpan("payment.service.approve")
    public Result approve(PaymentPayload paymentPayload) {
        return tossPayment.approve(paymentPayload);
    }

    @Transactional
    @WithSpan("payment.service.finalize_payment_payload")
    public void finalizePaymentPayload(Result approveResult) {
        if (approveResult instanceof SuccessResult successResult) {
            applySuccessResult(successResult);
            return;
        }
        if (approveResult instanceof FailedResult failedResult) {
            applyFailedResult(failedResult);
            return;
        }
        throw new BusinessException(UNKNOWN_ERROR);
    }

    @Transactional
    @WithSpan("payment.service.recover_approve_failed")
    public void recoverApproveFailed(PaymentPayload paymentPayload) {
        int updatedRows = paymentRepository.markRejected(
                paymentPayload.getOrderCode(),
                PaymentStatus.APPROVING,
                PaymentStatus.REJECTED,
                "retryFailed : unknown error"
        );
        validateApprovingPaymentUpdatedRows(updatedRows);
    }

    @WithSpan("payment.service.apply_success_result")
    private void applySuccessResult(SuccessResult successResult) {
        int updatedRows = paymentRepository.markApproved(
                successResult.orderCode(),
                PaymentStatus.APPROVING,
                PaymentStatus.APPROVED,
                successResult.totalAmount(),
                successResult.approvedAt(),
                successResult.receipt().url()
        );
        validateApprovingPaymentUpdatedRows(updatedRows);
    }

    @WithSpan("payment.service.apply_failed_result")
    private void applyFailedResult(FailedResult failedResult) {
        int updatedRows = paymentRepository.markRejected(
                failedResult.orderCode(),
                PaymentStatus.APPROVING,
                PaymentStatus.REJECTED,
                failedResult.message()
        );
        validateApprovingPaymentUpdatedRows(updatedRows);
    }

    @WithSpan("payment.service.validate_mark_approving_updated_rows")
    private void validateMarkApprovingUpdatedRows(int updatedRows, ApprovalReq approvalReq) {
        if (updatedRows == 1) return;

        boolean pendingPaymentExists = paymentRepository
                .findByOrderCodeAndPaymentStatus(approvalReq.orderCode(), PaymentStatus.PENDING)
                .isPresent();
        if (pendingPaymentExists) throw new BusinessException(PAYMENT_INVALID);
        throw new BusinessException(PAYMENT_NOT_FOUND);
    }

    @WithSpan("payment.service.validate_approving_payment_updated_rows")
    private void validateApprovingPaymentUpdatedRows(int updatedRows) {
        if (updatedRows != 1) throw new BusinessException(PAYMENT_NOT_FOUND);
    }
}
