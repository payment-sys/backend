package com.v_payment.pay.payment.infra;

import com.v_payment.pay.payment.infra.result.Result;

public interface PaymentStatusTranslator {
    Result translate(PaymentConfirmRes paymentConfirmRes, String fallbackOrderCode);
}
