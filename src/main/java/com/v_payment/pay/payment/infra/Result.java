package com.v_payment.pay.payment.infra;

public sealed interface Result permits SuccessResult, FailedResult{
}
