package com.v_payment.pay.order.controller.dto.res;

import com.v_payment.pay.order.entity.Order;
import com.v_payment.pay.order.entity.OrderStatus;

import java.util.List;

public record OrderCreateRes(
        String orderId,
        OrderStatus orderStatus,
        Long totalAmount,
        List<OrderItemRes> items
) {
    public static OrderCreateRes from(Order order) {
        return new OrderCreateRes(
                order.getOrderId(),
                order.getOrderStatus(),
                order.getTotalAmount(),
                order.getOrderItems().stream()
                        .map(OrderItemRes::from)
                        .toList()
        );
    }
}
