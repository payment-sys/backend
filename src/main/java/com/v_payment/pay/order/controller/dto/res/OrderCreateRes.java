package com.v_payment.pay.order.controller.dto.res;

import com.v_payment.pay.order.entity.Order;
import com.v_payment.pay.order.entity.OrderStatus;

import java.util.List;

public record OrderCreateRes(
        String orderCode
) {
    public static OrderCreateRes from(String orderCode) {
        return new OrderCreateRes(orderCode);
    }
}
