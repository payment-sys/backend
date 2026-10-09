package com.v_payment.pay.order.infrastructure.persistence.repository;

import com.v_payment.pay.order.domain.order.OrderStatus;

import java.util.List;

public interface OrderJdbcRepository {
    void updateSummaryBatch(List<OrderSummaryUpdate> updates);

    record OrderSummaryUpdate(
            String orderCode,
            OrderStatus targetStatus,
            List<Long> successProductIds,
            List<Long> failedProductIds
    ) {
        public static OrderSummaryUpdate of(String orderCode, OrderStatus targetStatus, List<Long> successProductIds, List<Long> failedProductIds) {
            return new OrderSummaryUpdate(orderCode, targetStatus, successProductIds, failedProductIds);
        }
    }
}
