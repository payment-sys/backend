package com.v_payment.pay.payment.repository;

public interface PaymentOutboxTaskProjection {
    Long getPaymentOutboxId();

    String getOrderId();

    String getPaymentKey();

    Long getAmount();
}
