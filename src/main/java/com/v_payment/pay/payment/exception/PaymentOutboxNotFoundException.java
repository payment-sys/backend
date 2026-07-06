package com.v_payment.pay.payment.exception;

import com.v_payment.pay.payment.entity.Payment;

public class PaymentOutboxNotFoundException extends RuntimeException {
    public PaymentOutboxNotFoundException(String message) {
        super(message);
    }
}
