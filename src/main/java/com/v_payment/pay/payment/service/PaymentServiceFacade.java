package com.v_payment.pay.payment.service;

import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.res.ApprovalRes;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.infra.Result;
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

    public CompletableFuture<ApprovalRes> approvePipeline(ApprovalReq approvalReq) {
        return CompletableFuture.supplyAsync(() -> approvePipelineInternal(approvalReq), paymentExecutorService);
    }

    private ApprovalRes approvePipelineInternal(ApprovalReq approvalReq) {
        PaymentPayload paymentPayload = paymentService.validateApprovalReq(approvalReq);

        Result result = paymentService.approve(paymentPayload);

        return paymentService.finalizePaymentPayload(result);
    }
}
