package com.v_payment.pay.payment.infra;

import com.v_payment.pay.payment.entity.Payment;

public enum PaymentError {
    NETWORK_TIMEOUT, UPSTREAM_429, UPSTREAM_5XX, UPSTREAM_4XX, UNKNOWN;
}
