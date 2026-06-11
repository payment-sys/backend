package com.v_payment.pay.payment.service.outbox.exception;

public class PaymentOutboxNotFoundException extends RuntimeException {
    public PaymentOutboxNotFoundException(String message) {
        super(message);
    }
}
