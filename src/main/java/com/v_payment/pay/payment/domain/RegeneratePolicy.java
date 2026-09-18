package com.v_payment.pay.payment.domain;

import com.v_payment.pay.payment.domain.entity.Payment;
import com.v_payment.pay.payment.domain.entity.PaymentStatus;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class RegeneratePolicy {
    private static final Set<PaymentStatus> REGENERATABLE_STATUSES = Set.of(
            PaymentStatus.READY,
            PaymentStatus.ABORTED,
            PaymentStatus.EXPIRED
    );

    private final boolean orderCreated;
    private final List<Payment> payments;

    private RegeneratePolicy(boolean orderCreated, Collection<Payment> payments) {
        this.orderCreated = orderCreated;
        this.payments = List.copyOf(validatePayments(payments));
    }

    public boolean canRegenerate() {
        return orderCreated && hasPayments() && payments.stream()
                .allMatch(payment -> REGENERATABLE_STATUSES.contains(payment.getPaymentStatus()));
    }

    public List<PaymentStatus> getRegeneratableStatuses() {
        return List.copyOf(REGENERATABLE_STATUSES);
    }

    public boolean hasPayments() {
        return !payments.isEmpty();
    }

    public static RegeneratePolicy of(boolean orderCreated, Collection<Payment> payments) {
        return new RegeneratePolicy(orderCreated, payments);
    }

    private Collection<Payment> validatePayments(Collection<Payment> payments) {
        if (payments == null) throw new IllegalArgumentException("payments는 필수입니다.");
        return payments;
    }
}
