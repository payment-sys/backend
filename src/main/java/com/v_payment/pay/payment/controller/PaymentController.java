package com.v_payment.pay.payment.controller;

import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.req.TossPaymentWebhookReq;
import com.v_payment.pay.payment.controller.dto.res.ApprovalRes;
import com.v_payment.pay.payment.service.PaymentServiceFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * unit test
 * DisplayName: 결제 승인 요청이 올 시 paymentServiceFacade에 정상적으로 위임 후 return 받을 수 있다.
 * Given : 유효한 ApprovalReq, PaymentServiceFacade는 CompletableFuture<ApprovalRes> 응답 Stub
 * When : approve() 호출
 * Then : approvePipeLine()이 호출돼야 한다.CompletableFuture<ApprovalRes>가 응답돼야 한다.
 *
 * DisplayName: 결제 상태 변경 웹 훅이 올 시 paymentServiceFacade에 정상적으로 위임 후 return 받을 수 있다.
 * Given : 유효한 TossPaymentWebhookReq, PaymentServiceFacade 모킹
 * When : syncTossPaymentStatus 호출
 * Then : facade.syncTossPaymentStatus()가 호출돼야 한다.
 */
@Slf4j(topic = "API_LOGGER")
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentServiceFacade paymentServiceFacade;

    @PostMapping("/approvals")
    public CompletableFuture<ApprovalRes> approve(
            @RequestBody ApprovalReq approvalReq
    ) {
        return paymentServiceFacade.approvePipeline(approvalReq);
    }

    @PostMapping("/webhooks/toss")
    public void syncTossPaymentStatus(
            @RequestBody TossPaymentWebhookReq webhookReq
    ) {
        paymentServiceFacade.syncTossPaymentStatus(webhookReq);
    }
}
