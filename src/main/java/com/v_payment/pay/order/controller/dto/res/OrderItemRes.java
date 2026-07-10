package com.v_payment.pay.order.controller.dto.res;

import com.v_payment.pay.order.entity.OrderItem;

public record OrderItemRes(
        Long productId,
        String productName,
        Long unitPrice,
        Integer quantity,
        Long orderAmount
) {
    public static OrderItemRes from(OrderItem orderItem) {
        return new OrderItemRes(
                orderItem.getProductId(),
                orderItem.getProductName(),
                orderItem.getUnitPrice(),
                orderItem.getQuantity(),
                orderItem.getOrderAmount()
        );
    }
}
