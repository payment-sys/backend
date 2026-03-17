package com.v_payment.pay.payment.controller.dto.res;

import java.math.BigDecimal;

public record PaymentCreateRes(
        String orderId,
        String orderName,
        BigDecimal amount
) {}
