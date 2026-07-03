package com.v_payment.pay.payment.outbox;

import com.v_payment.pay.payment.outbox.entity.PaymentPayload;

public record PaymentOutboxTask(Long id, PaymentPayload paymentPayload) {
}
