package com.v_payment.pay.payment.service;

import com.v_payment.pay.event.repository.PaymentEventRepository;
import com.v_payment.pay.event.entity.PaymentEvent;
import com.v_payment.pay.global.BusinessException;
import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.req.PaymentCreateReq;
import com.v_payment.pay.payment.controller.dto.res.PaymentCreateRes;
import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.exception.PaymentException;
import com.v_payment.pay.payment.repository.PaymentRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final Clock clock;
    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;

    public PaymentCreateRes create(PaymentCreateReq paymentCreateReq) {
        Payment newPayment = Payment.create(paymentCreateReq, clock);

        Payment savedPayment = paymentRepository.save(newPayment);

        return PaymentCreateRes.from(savedPayment);
    }

    public void approve(ApprovalReq approvalReq) {
        Payment payment = paymentRepository.findByOrderId(approvalReq.orderId(), PaymentStatus.PENDING)
            .orElseThrow(() -> new BusinessException(PaymentException.PAYMENT_NOT_FOUND));

        PaymentPayload paymentPayload = payment.getPaymentPayload();

        PaymentEvent paymentEvent = PaymentEvent.create(paymentPayload, payment);

        paymentEventRepository.save(paymentEvent);
    }
}
