package com.v_payment.pay.order.infrastructure.persistence.repository;

import com.v_payment.pay.order.domain.order.OrderStatus;
import com.v_payment.pay.order.domain.order.OrderItemInfo;

import java.util.List;

public interface OrderJdbcRepository {
    void updateSummaryBatch(List<OrderSummaryUpdate> updates);

    record OrderSummaryUpdate(
            String orderCode,
            OrderStatus targetStatus,
            List<OrderItemInfo> orderItems,
            Long totalAmount
    ) {
        public static OrderSummaryUpdate of(
                String orderCode,
                OrderStatus targetStatus,
                List<OrderItemInfo> orderItems,
                Long totalAmount
        ) {
            return new OrderSummaryUpdate(orderCode, targetStatus, orderItems, totalAmount);
        }
    }
}
