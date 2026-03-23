package com.v_payment.pay.payment.controller;

import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.req.PaymentCreateReq;
import com.v_payment.pay.payment.controller.dto.res.ApprovalRes;
import com.v_payment.pay.payment.controller.dto.res.PaymentCreateRes;
import com.v_payment.pay.payment.service.PaymentService;
import com.v_payment.pay.payment.service.PaymentServiceFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return paymentService.create(paymentCreateReq);
    }

    @PostMapping("/approvals")
    public ApprovalRes approve(
            @RequestBody ApprovalReq approvalReq
    ) {
        return paymentServiceFacade.approvePipeline(approvalReq);
    }
}
