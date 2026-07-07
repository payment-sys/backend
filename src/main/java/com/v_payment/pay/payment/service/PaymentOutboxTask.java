package com.v_payment.pay.payment.service;

import com.v_payment.pay.payment.entity.PaymentPayload;


public record PaymentOutboxTask(Long id, PaymentPayload paymentPayload) {
}
