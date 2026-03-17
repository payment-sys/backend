package com.v_payment.pay.payment.service;

import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.req.PaymentCreateReq;
import com.v_payment.pay.payment.controller.dto.res.ApprovalRes;
import com.v_payment.pay.payment.controller.dto.res.PaymentCreateRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {
    public PaymentCreateRes create(PaymentCreateReq paymentCreateReq) {
        return null;
    }

    public ApprovalRes approve(ApprovalReq approvalReq) {
        return null;
    }
}
