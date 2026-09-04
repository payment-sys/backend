package com.v_payment.pay.payment.service;

import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.req.TossPaymentWebhookReq;
import com.v_payment.pay.payment.controller.dto.res.ApprovalRes;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.infra.result.Result;
import com.v_payment.pay.payment.infra.toss.TossPayment;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j(topic = "API_LOGGER")
@Component
@RequiredArgsConstructor
public class PaymentServiceFacade {
    private final TossPayment tossPayment;
    private final PaymentService paymentService;
    private final ExecutorService paymentExecutorService;
    private final MeterRegistry meterRegistry;

    public CompletableFuture<ApprovalRes> approvePipeline(ApprovalReq approvalReq) {
        return CompletableFuture.supplyAsync(() -> approvePipelineInternal(approvalReq), paymentExecutorService);
    }

    private ApprovalRes approvePipelineInternal(ApprovalReq approvalReq) {
        PaymentPayload paymentPayload = recordStage("validate", () -> validatePaymentPayload(approvalReq));
        Result result = recordStage("external_confirm", () -> approve(paymentPayload));
        return recordStage("finalize", () -> finalize(result));
    }

    @WithSpan("payment.service.validate_payment_payload")
    private PaymentPayload validatePaymentPayload(ApprovalReq approvalReq) {
        return paymentService.validateApprovalReq(approvalReq);
    }

    @WithSpan("payment.service.approve")
    private Result approve(PaymentPayload paymentPayload) {
        return tossPayment.approve(paymentPayload);
    }

    @WithSpan("payment.service.finalize_payment_payload")
    private ApprovalRes finalize(Result result) {
        return paymentService.finalizePaymentPayload(result);
    }

    @WithSpan("payment.service.sync_toss_payment_status")
    public void syncTossPaymentStatus(TossPaymentWebhookReq webhookReq) {
        paymentService.syncTossPaymentStatus(webhookReq);
    }

    private <T> T recordStage(String stage, Supplier<T> supplier) {
        long startedAtNanos = System.nanoTime();
        String outcome = "SUCCESS";
        String error = "none";
        try {
            return supplier.get();
        } catch (RuntimeException e) {
            outcome = "ERROR";
            error = e.getClass().getSimpleName();
            throw e;
        } finally {
            Timer.builder("pay.payment.approval.stage.duration")
                    .description("Payment approval pipeline stage duration")
                    .tag("stage", stage)
                    .tag("outcome", outcome)
                    .tag("error", error)
                    .register(meterRegistry)
                    .record(System.nanoTime() - startedAtNanos, TimeUnit.NANOSECONDS);
        }
    }
}
