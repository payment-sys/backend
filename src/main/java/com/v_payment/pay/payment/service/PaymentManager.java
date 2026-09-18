package com.v_payment.pay.payment.service;

import com.v_payment.pay.payment.domain.entity.Payment;
import com.v_payment.pay.payment.domain.entity.PaymentMethod;
import com.v_payment.pay.payment.repository.PaymentRepository;
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

    public void createPendingPayments(Collection<PendingPaymentCreateRequest> requests) {
        if (requests.isEmpty()) return;

        List<Payment> payments = requests.stream()
                .map(r -> Payment.createPendingPayment(r.orderCode(), r.amount(),
                        r.paymentMethod(), clock))
                .toList();

        paymentRepository.saveReadyPayments(payments);
    }

    public record PendingPaymentCreateRequest(
            String orderCode,
            Long amount,
            PaymentMethod paymentMethod
    ) {
    }
}
