package com.v_payment.pay.order.service;

import com.v_payment.pay.global.exception.BusinessException;
import com.v_payment.pay.order.entity.Order;
import com.v_payment.pay.order.entity.OrderItem;
import com.v_payment.pay.order.entity.OrderStatus;
import com.v_payment.pay.order.exception.OrderException;
import com.v_payment.pay.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderManager {
    private final OrderRepository orderRepository;

    public OrderStatusUpdateResult updateStatus(String orderCode, OrderStatus orderStatus) {
        Order order = orderRepository.findByOrderCodeWithItemsForUpdate(orderCode)
                .orElseThrow(() -> new BusinessException(OrderException.ORDER_NOT_FOUND));

        if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
            return OrderStatusUpdateResult.notUpdated();
        }

        order.updateStatus(orderStatus);
        return OrderStatusUpdateResult.updated(order.getOrderItems().stream()
                .map(OrderItemSnapshot::from)
                .toList());
    }

    public record OrderStatusUpdateResult(
            boolean updated,
            List<OrderItemSnapshot> orderItems
    ) {
        private static OrderStatusUpdateResult updated(List<OrderItemSnapshot> orderItems) {
            return new OrderStatusUpdateResult(true, orderItems);
        }

        private static OrderStatusUpdateResult notUpdated() {
            return new OrderStatusUpdateResult(false, List.of());
        }
    }

    public record OrderItemSnapshot(
            Long productId,
            Integer quantity
    ) {
        private static OrderItemSnapshot from(OrderItem orderItem) {
            return new OrderItemSnapshot(orderItem.getProductId(), orderItem.getQuantity());
        }
    }
}
