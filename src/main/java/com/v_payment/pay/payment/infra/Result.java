package com.v_payment.pay.payment.infra;

import com.v_payment.pay.payment.entity.Payment;

public sealed interface Result permits SuccessResult, FailedResult{
}
