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

    public void createPendingPayment(String orderCode, Long amount, PaymentMethod paymentMethod) {
        paymentRepository.save(Payment.createPendingPayment(orderCode, amount, paymentMethod, clock));
    }
}
