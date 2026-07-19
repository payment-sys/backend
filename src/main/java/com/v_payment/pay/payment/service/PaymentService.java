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
        validateApprovingPaymentUpdatedRows(updatedRows);

        return PaymentPayload.create(approvalReq.orderCode(), approvalReq.paymentKey(), approvalReq.requestedAmount());
    }

    @WithSpan("payment.service.approve")
    public Result approve(PaymentPayload paymentPayload) {
        return tossPayment.approve(paymentPayload);
    }

    @Transactional
    @WithSpan("payment.service.finalize_payment_payload")
    public ApprovalRes finalizePaymentPayload(Result approveResult) {
        if (approveResult instanceof SuccessResult successResult) {
            return applySuccessResult(successResult);
        }
        if (approveResult instanceof FailedResult failedResult) {
            return applyFailedResult(failedResult);
        }
        throw new BusinessException(UNKNOWN_ERROR);
    }

    @WithSpan("payment.service.apply_success_result")
    private ApprovalRes applySuccessResult(SuccessResult successResult) {
        int updatedRows = paymentRepository.markApproved(
                successResult.orderCode(),
                PaymentStatus.APPROVING,
                PaymentStatus.APPROVED,
                successResult.totalAmount(),
                successResult.approvedAt(),
                successResult.receipt().url()
        );
        validateApprovingPaymentUpdatedRows(updatedRows);
        return ApprovalRes.from(successResult);
    }

    @WithSpan("payment.service.apply_failed_result")
    private ApprovalRes applyFailedResult(FailedResult failedResult) {
        int updatedRows = paymentRepository.markRejected(
                failedResult.orderCode(),
                PaymentStatus.APPROVING,
                PaymentStatus.REJECTED,
                failedResult.message()
        );
        validateApprovingPaymentUpdatedRows(updatedRows);
        return ApprovalRes.from(failedResult);
    }

    @WithSpan("payment.service.validate_approving_payment_updated_rows")
    private void validateApprovingPaymentUpdatedRows(int updatedRows) {
        if (updatedRows != 1) throw new BusinessException(PAYMENT_NOT_FOUND);
    }
}
