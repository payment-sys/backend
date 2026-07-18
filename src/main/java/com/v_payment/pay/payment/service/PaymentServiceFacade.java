package com.v_payment.pay.payment.service;

import com.v_payment.pay.global.ExecutorWithRetry;
import com.v_payment.pay.payment.config.PaymentExecutorConfig;
import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.res.ApprovalRes;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.PaymentError;
import com.v_payment.pay.payment.infra.Result;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j(topic = "API_LOGGER")
@Component
@RequiredArgsConstructor
public class PaymentServiceFacade {
    private final PaymentService paymentService;
    private final ExecutorService paymentExecutorService;

    public CompletableFuture<ApprovalRes> approvePipeline(ApprovalReq approvalReq) {
        return CompletableFuture.supplyAsync(() -> {
            PaymentPayload payload = paymentService.validateApprovalReq(approvalReq);
            Result result = getApproveResult(payload);
            return ApprovalRes.from(paymentService.finalizePaymentPayload(result));
        }, paymentExecutorService);
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
