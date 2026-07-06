package com.v_payment.pay.payment.exception;

import com.v_payment.pay.payment.entity.Payment;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
