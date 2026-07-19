package com.v_payment.pay.payment.controller;

import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.res.ApprovalRes;
import com.v_payment.pay.payment.service.PaymentServiceFacade;
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
    private final PaymentServiceFacade paymentServiceFacade;

    @PostMapping("/approvals")
    @WithSpan("payment.controller.approve")
    public CompletableFuture<ApprovalRes> approve(
            @RequestBody ApprovalReq approvalReq
    ) {
        return paymentServiceFacade.approvePipeline(approvalReq);
    }
}
