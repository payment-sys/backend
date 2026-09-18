package com.v_payment.pay.payment.service;

import com.v_payment.pay.global.exception.BusinessException;
import com.v_payment.pay.order.service.OrderManager;
import com.v_payment.pay.payment.controller.dto.req.PaymentRegenerateReq;
import com.v_payment.pay.payment.controller.dto.res.PaymentRegenerateRes;
import com.v_payment.pay.payment.domain.RegeneratePolicy;
import com.v_payment.pay.payment.domain.entity.Payment;
import com.v_payment.pay.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.v_payment.pay.payment.exception.PaymentException.PAYMENT_NOT_FOUND;
import static com.v_payment.pay.payment.exception.PaymentException.PAYMENT_REGENERATION_NOT_ALLOWED;

@Service
@RequiredArgsConstructor
public class PaymentRegenerationService {
    private final Clock clock;
    private final PaymentRepository paymentRepository;
    private final OrderManager orderManager;

    @Transactional
    public PaymentRegenerateRes regeneratePayment(PaymentRegenerateReq req) {
        List<Payment> payments = paymentRepository.findAllByOrderCode(req.orderCode());
        if (payments.isEmpty()) throw new BusinessException(PAYMENT_NOT_FOUND);

        RegeneratePolicy regeneratePolicy = RegeneratePolicy.of(orderManager.isCreatedStatus(req.orderCode()), payments);
        if (!regeneratePolicy.canRegenerate()) throw new BusinessException(PAYMENT_REGENERATION_NOT_ALLOWED);

        Payment source = payments.stream()
                .max(Comparator.comparing(Payment::getRequestedAt))
                .orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));

        expireReadyPayments(payments);

        Payment regeneratedPayment = Payment.createReady(req.orderCode(), UUID.randomUUID().toString(),
                source.getRequestedAmount(), source.getPaymentMethod(), clock);

        paymentRepository.save(regeneratedPayment);

        return PaymentRegenerateRes.ready(req.orderCode(), regeneratedPayment.getIdempotencyKey());
    }

    private void expireReadyPayments(List<Payment> payments) {
        payments.stream()
                .filter(Payment::isReady)
                .forEach(Payment::markExpired);
    }
}
