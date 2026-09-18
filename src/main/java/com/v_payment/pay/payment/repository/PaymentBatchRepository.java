package com.v_payment.pay.payment.repository;

import com.v_payment.pay.payment.domain.entity.Payment;

import java.util.Collection;

public interface PaymentBatchRepository {
    void saveReadyPayments(Collection<Payment> payments);
}
