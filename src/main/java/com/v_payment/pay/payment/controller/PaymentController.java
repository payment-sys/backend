package com.v_payment.pay.payment.controller;

import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.req.PaymentRegenerateReq;
import com.v_payment.pay.payment.controller.dto.req.TossPaymentWebhookReq;
import com.v_payment.pay.payment.controller.dto.res.ApprovalRes;
import com.v_payment.pay.payment.controller.dto.res.PaymentRegenerateRes;
import com.v_payment.pay.payment.service.PaymentApprovalServiceFacade;
import com.v_payment.pay.payment.service.PaymentRegenerationService;
import com.v_payment.pay.payment.service.PaymentWebhookService;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@Slf4j(topic = "API_LOGGER")
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentApprovalServiceFacade paymentApprovalServiceFacade;
    private final PaymentWebhookService paymentWebhookService;
    private final PaymentRegenerationService paymentRegenerationService;

    @WithSpan("payment.PaymentController.approve")
    @PostMapping("/approvals")
    public CompletableFuture<ApprovalRes> approve(
            @RequestBody ApprovalReq approvalReq
    ) {
        return paymentApprovalServiceFacade.approvePipeline(approvalReq);
    }

    @PostMapping("/webhooks/toss")
    public void syncTossPaymentStatus(
            @RequestBody TossPaymentWebhookReq webhookReq
    ) {
        paymentWebhookService.syncTossPaymentStatus(webhookReq);
    }

    @PostMapping("/regenerations")
    public PaymentRegenerateRes regenerate(
            @RequestBody PaymentRegenerateReq req
    ) {
        return paymentRegenerationService.regeneratePayment(req);
    }
}
