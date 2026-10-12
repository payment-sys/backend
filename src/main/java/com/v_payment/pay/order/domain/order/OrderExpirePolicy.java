package com.v_payment.pay.order.domain.order;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class OrderExpirePolicy {
    public List<Order> excludePaymentInProgressOrders(List<Order> orders, Collection<String> paymentInProgressOrderCodes) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }
        if (paymentInProgressOrderCodes == null || paymentInProgressOrderCodes.isEmpty()) {
            return List.copyOf(orders);
        }

        Set<String> excludedOrderCodes = Set.copyOf(paymentInProgressOrderCodes);
        return orders.stream()
                .filter(order -> !excludedOrderCodes.contains(order.getOrderCode()))
                .toList();
    }

    public Map<Long, Integer> successQuantities(Order order) {
        if (order == null || order.getOrderItemInfos() == null || order.getOrderItemInfos().isEmpty()) {
            return Map.of();
        }

        return order.getOrderItemInfos().stream()
                .filter(orderItem -> orderItem.getStatus() == OrderItemStatus.CHANGED)
                .collect(Collectors.toMap(
                        OrderItemInfo::getProductId,
                        OrderItemInfo::getQuantity,
                        Integer::sum
                ));
    }
}
