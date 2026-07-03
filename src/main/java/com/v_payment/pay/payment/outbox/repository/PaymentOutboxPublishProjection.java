package com.v_payment.pay.payment.outbox.repository;

public interface PaymentOutboxPublishProjection {
    Long getPaymentOutboxId();

    String getOrderId();

    String getPaymentKey();

    Long getAmount();
}
