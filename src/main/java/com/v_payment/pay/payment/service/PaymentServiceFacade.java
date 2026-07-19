package com.v_payment.pay.payment.service;

import com.v_payment.pay.global.ExecutorWithRetry;
import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.res.ApprovalRes;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.PaymentError;
import com.v_payment.pay.payment.infra.Result;
import com.v_payment.pay.payment.limiter.Limiter;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j(topic = "API_LOGGER")
@Component
@RequiredArgsConstructor
public class PaymentServiceFacade {
    private final PaymentService paymentService;
    private final ExecutorService paymentExecutorService;
    private final Limiter paymentResultApplyLimiter;

    @WithSpan("payment.facade.approve_pipeline")
    public CompletableFuture<ApprovalRes> approvePipeline(ApprovalReq approvalReq) {
        return CompletableFuture.supplyAsync(() -> approvePipelineInternal(approvalReq), paymentExecutorService);
    }

    @WithSpan("payment.facade.approve_pipeline_internal")
    private ApprovalRes approvePipelineInternal(ApprovalReq approvalReq) {
        PaymentPayload payload = paymentService.validateApprovalReq(approvalReq);
        Result result = getApproveResult(payload);
        paymentService.finalizePaymentPayload(result);
        return ApprovalRes.from(result);
    }

    @WithSpan("payment.facade.get_approve_result")
    private Result getApproveResult(PaymentPayload paymentPayload) {
        return ExecutorWithRetry
                .task(() -> paymentService.approve(paymentPayload))
                .continueCondition(this::getContinueCondition)
                .delayMillis(1000)
                .maxAttempts(3)
                .recovery(() -> paymentService.recoverApproveFailed(paymentPayload))
                .execute();
    }

    @WithSpan("payment.facade.retry_continue_condition")
    private boolean getContinueCondition(Result result) {
        if (result instanceof FailedResult failedResult) {
            return failedResult.paymentError() == PaymentError.NETWORK_TIMEOUT
                    || failedResult.paymentError() == PaymentError.UPSTREAM_429
                    || failedResult.paymentError() == PaymentError.UPSTREAM_5XX;
        }
        return false;
    }
}
