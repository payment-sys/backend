package com.v_payment.pay.payment.service.outbox;

public class PaymentOutboxError extends RuntimeException {
    public PaymentOutboxError(String message) {
        super(message);
    }
}
