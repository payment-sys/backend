package com.v_payment.pay.payment.service;

import com.v_payment.pay.global.exception.BusinessException;
import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.Result;
import com.v_payment.pay.payment.infra.SuccessResult;
import com.v_payment.pay.payment.infra.TossPayment;
import com.v_payment.pay.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
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
    public PaymentPayload validateApprovalReq(ApprovalReq approvalReq) {
        Payment payment = paymentRepository.findByOrderIdAndPaymentStatus(approvalReq.orderId(), PaymentStatus.PENDING)
                .orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));

        if (!payment.isSameRequestedAmount(approvalReq.requestedAmount())) throw new BusinessException(PAYMENT_INVALID);
        if (!payment.isSameMethod(approvalReq.method())) throw new BusinessException(PAYMENT_INVALID);
        if (!payment.isSameProvider(approvalReq.provider())) throw new BusinessException(PAYMENT_INVALID);

        try {
            payment.completeValidate(approvalReq);
            paymentRepository.flush();
        } catch (OptimisticLockingFailureException e) {
            throw new BusinessException(PAYMENT_INVALID);
        }

        return payment.getPaymentPayload();
    }

    public Result approve(PaymentPayload paymentPayload) {
        return tossPayment.approve(paymentPayload);
    }

    @Transactional
    public Payment finalizePaymentPayload(Result approveResult) {
        if (approveResult instanceof SuccessResult successResult) {
            return applySuccessResult(successResult);
        }
        if (approveResult instanceof FailedResult failedResult) {
            return applyFailedResult(failedResult);
        }
        throw new BusinessException(UNKNOWN_ERROR);
    }

    @Transactional
    public void recoverApproveFailed(PaymentPayload paymentPayload) {
        Payment retryFailedPayment = paymentRepository.findByOrderIdAndPaymentStatus(
                paymentPayload.getOrderId(),
                PaymentStatus.APPROVING
        ).orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));

        try {
            retryFailedPayment.retryFailed();
            paymentRepository.flush();
        } catch (OptimisticLockingFailureException e) {
            throw new BusinessException(PAYMENT_INVALID);
        }
    }

    private Payment applySuccessResult(SuccessResult successResult) {
        Payment successedPayment = paymentRepository.findByOrderIdAndPaymentStatus(
                successResult.orderId(),
                PaymentStatus.APPROVING
        ).orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));

        try {
            successedPayment.success(successResult);
            paymentRepository.flush();
            return successedPayment;
        } catch (OptimisticLockingFailureException e) {
            throw new BusinessException(PAYMENT_INVALID);
        }
    }

    private Payment applyFailedResult(FailedResult failedResult) {
        Payment failedPayment = paymentRepository.findByOrderIdAndPaymentStatus(
                failedResult.orderId(),
                PaymentStatus.APPROVING
        ).orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));

        try {
            failedPayment.failed(failedResult);
            paymentRepository.flush();
            return failedPayment;
        } catch (OptimisticLockingFailureException e) {
            throw new BusinessException(PAYMENT_INVALID);
        }
    }
}
