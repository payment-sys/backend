package com.v_payment.pay.payment.controller;

import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.req.PaymentCreateReq;
import com.v_payment.pay.payment.controller.dto.res.PaymentCreateRes;
import com.v_payment.pay.payment.service.PaymentService;
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

    @PostMapping
    public PaymentCreateRes createPayment(
            @RequestBody PaymentCreateReq paymentCreateReq
    ){
        return paymentService.create(paymentCreateReq);
    }

    @PostMapping("/approvals")
    public void approveV2(
            @RequestBody ApprovalReq approvalReq
    ) {
        paymentService.approve(approvalReq);
    }
}
