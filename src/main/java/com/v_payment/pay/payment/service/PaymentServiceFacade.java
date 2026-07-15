package com.v_payment.pay.payment.service;

import com.v_payment.pay.global.ExecutorWithRetry;
import com.v_payment.pay.global.LTimer;
import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.res.ApprovalRes;
import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.PaymentError;
import com.v_payment.pay.payment.infra.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j(topic = "API_LOGGER")
@Component
@RequiredArgsConstructor
public class PaymentServiceFacade {
    private static final long DB_CONNECTION_TIMEOUT = 2000;
    private static final long PAYMENT_API_TIMEOUT = 1500;

    private final PaymentService paymentService;

    public ApprovalRes approvePipeline(ApprovalReq approvalReq) {
        long vStartTime = LTimer.getCurrTime();
        PaymentPayload paymentPayload = paymentService.validateApprovalReq(approvalReq);
        log.debug("approval validation [{}] latency = {}", approvalReq.orderCode(), LTimer.getDiff(vStartTime));
        if (LTimer.getDiff(vStartTime) >= DB_CONNECTION_TIMEOUT) {
            log.warn("validateApprovalReq() is slow. orderCode = {}", approvalReq.orderCode());
        }

        long cStartTime = LTimer.getCurrTime();
        Result approveResult = getApproveResult(paymentPayload);
        log.debug("approval call [{}] latency = {}", approvalReq.orderCode(), LTimer.getDiff(cStartTime));
        if (LTimer.getDiff(cStartTime) >= PAYMENT_API_TIMEOUT) {
            log.warn("payment API call is slow. orderCode = {}", approvalReq.orderCode());
        }

        long fStartTime = LTimer.getCurrTime();
        Payment finishedPayment = paymentService.finalizePaymentPayload(approveResult);
        log.debug("approval result apply [{}] latency = {}", approvalReq.orderCode(), LTimer.getDiff(fStartTime));
        if (LTimer.getDiff(fStartTime) >= DB_CONNECTION_TIMEOUT) {
            log.warn("finalizePaymentPayload() is slow. orderCode = {}", approvalReq.orderCode());
        }

        return ApprovalRes.from(finishedPayment);
    }

    private Result getApproveResult(PaymentPayload paymentPayload) {
        return ExecutorWithRetry
                .task(() -> paymentService.approve(paymentPayload))
                .continueCondition(this::getContinueCondition)
                .delayMillis(1000)
                .maxAttempts(3)
                .recovery(() -> paymentService.recoverApproveFailed(paymentPayload))
                .execute();
    }

    private boolean getContinueCondition(Result result) {
        if (result instanceof FailedResult failedResult) {
            return failedResult.paymentError() == PaymentError.NETWORK_TIMEOUT
                    || failedResult.paymentError() == PaymentError.UPSTREAM_429
                    || failedResult.paymentError() == PaymentError.UPSTREAM_5XX;
        }
        return false;
    }
}
