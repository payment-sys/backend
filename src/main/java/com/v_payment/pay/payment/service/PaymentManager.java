package com.v_payment.pay.payment.service;

import com.v_payment.pay.order.domain.PaymentCreateSource;
import com.v_payment.pay.payment.domain.entity.Payment;
import com.v_payment.pay.payment.domain.entity.PaymentMethod;
import com.v_payment.pay.payment.repository.PaymentRepository;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentManager {
    private final Clock clock;
    private final PaymentRepository paymentRepository;

    @WithSpan("payment.PaymentManager.createPendingPayments")
    public void createPendingPayments(Collection<PaymentCreateSource> paymentCreateSources) {
        if (paymentCreateSources.isEmpty()) return;

        List<Payment> payments = paymentCreateSources.stream()
                .map(r -> Payment.createPendingPayment(r.orderCode(), r.amount(),
                        r.paymentMethod(), clock))
                .toList();

        paymentRepository.saveReadyPayments(payments);
    }
}
