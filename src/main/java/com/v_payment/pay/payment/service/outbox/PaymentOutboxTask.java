package com.v_payment.pay.payment.service.outbox;

import com.v_payment.pay.payment.entity.outbox.PaymentPayload;

public record PaymentOutboxTask(Long id, PaymentPayload paymentPayload) {
}
