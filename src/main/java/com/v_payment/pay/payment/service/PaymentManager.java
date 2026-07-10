package com.v_payment.pay.payment.service;

import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentMethod;
import com.v_payment.pay.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
@RequiredArgsConstructor
public class PaymentManager {
    private final Clock clock;
    private final PaymentRepository paymentRepository;
    private final PaymentLedgerService paymentLedgerService;

    public Payment createForOrder(String orderId, Long amount, PaymentMethod paymentMethod) {
        Payment payment = paymentRepository.save(Payment.createForOrder(orderId, amount, paymentMethod, clock));
        paymentLedgerService.insertPaymentLedgerPENDING(payment);
        return payment;
    }
}
