package com.v_payment.pay.payment.infra;

import com.v_payment.pay.payment.entity.PaymentPayload;

public interface Pay {
    Result approve(PaymentPayload paymentPayload);

    Result check(PaymentPayload paymentPayload);
}
