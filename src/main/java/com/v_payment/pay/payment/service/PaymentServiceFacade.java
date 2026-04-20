package com.v_payment.pay.payment.service;

import com.v_payment.pay.global.ExecutorWithRetry;
import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.res.ApprovalRes;
import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.PaymentError;
import com.v_payment.pay.payment.infra.Result;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j(topic = "API_LOGGER")
@Component
@RequiredArgsConstructor
public class PaymentServiceFacade {
    private final PaymentService paymentService;

    @Timed(value = "payment.tx.approve")
    public ApprovalRes approvePipeline(ApprovalReq approvalReq) {
        PaymentPayload paymentPayload = paymentService.validateApprovalReq(approvalReq);

        Result approveResult = getApproveResult(paymentPayload);

        Payment finishedPayment = paymentService.finalizePaymentPayload(approveResult);

        log.info("승인 성공");
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
        if(result instanceof FailedResult failedResult) {
            return failedResult.paymentError() == PaymentError.NETWORK_TIMEOUT ||
                    failedResult.paymentError() == PaymentError.UPSTREAM_429 ||
                    failedResult.paymentError() == PaymentError.UPSTREAM_5XX;
        }
        return false;
    }
}
