package com.v_payment.pay.payment.controller;

import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.req.PaymentCreateReq;
import com.v_payment.pay.payment.controller.dto.res.ApprovalRes;
import com.v_payment.pay.payment.controller.dto.res.PaymentCreateRes;
import com.v_payment.pay.payment.service.PaymentService;
import com.v_payment.pay.payment.service.PaymentServiceFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j(topic = "API_LOGGER")
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final PaymentServiceFacade paymentServiceFacade;

    @PostMapping
    public PaymentCreateRes createPayment(
            @RequestBody PaymentCreateReq paymentCreateReq
    ){
        log.info("요청 금액 = {}", paymentCreateReq.requestedAmount());
        return paymentService.create(paymentCreateReq);
    }

    @PostMapping("/approvals")
    public ApprovalRes approve(
            @RequestBody ApprovalReq approvalReq
    ) {
        log.info("승인 예정 금액 = {}", approvalReq.requestedAmount());
        return paymentServiceFacade.approvePipeline(approvalReq);
    }
}
