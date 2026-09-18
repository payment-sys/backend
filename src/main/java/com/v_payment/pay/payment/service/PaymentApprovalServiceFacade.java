package com.v_payment.pay.payment.service;

import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.res.ApprovalRes;
import com.v_payment.pay.payment.domain.entity.PaymentPayload;
import com.v_payment.pay.payment.infra.result.Result;
import com.v_payment.pay.payment.infra.toss.TossPayment;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j(topic = "API_LOGGER")
@Component
@RequiredArgsConstructor
public class PaymentApprovalServiceFacade {
    private final TossPayment tossPayment;
    private final PaymentApprovalService paymentApprovalService;
    private final ExecutorService paymentExecutorService;

    public CompletableFuture<ApprovalRes> approvePipeline(ApprovalReq approvalReq) {
        return CompletableFuture.supplyAsync(() -> approvePipelineInternal(approvalReq), paymentExecutorService);
    }

    private ApprovalRes approvePipelineInternal(ApprovalReq approvalReq) {
        PaymentPayload paymentPayload = validatePaymentPayload(approvalReq);
        Result result = approve(paymentPayload);
        return finalize(result);
    }

    @WithSpan("payment.service.validate_payment_payload")
    private PaymentPayload validatePaymentPayload(ApprovalReq approvalReq) {
        return paymentApprovalService.validateApprovalReq(approvalReq);
    }

    @WithSpan("payment.service.approve")
    private Result approve(PaymentPayload paymentPayload) {
        return tossPayment.approve(paymentPayload);
    }

    @WithSpan("payment.service.finalize_payment_payload")
    private ApprovalRes finalize(Result result) {
        return paymentApprovalService.finalizePaymentPayload(result);
    }
}
