package com.v_payment.pay.order.service;

import com.v_payment.pay.order.entity.OrderItem;
import com.v_payment.pay.order.repository.OrderItemRepository;
import com.v_payment.pay.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderManager {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public boolean markFailed(String orderCode) {
        return orderRepository.markFailed(orderCode) == 1;
    }

    public boolean markFailed(Collection<String> orderCodes) {
        if (orderCodes.isEmpty()) return true;
        return orderRepository.markFailedByOrderCodes(orderCodes) == orderCodes.size();
    }

    public List<OrderItemSnapshot> findOrderItems(String orderCode) {
        return orderItemRepository.findAllByOrderCode(orderCode).stream()
                .map(OrderItemSnapshot::from)
                .toList();
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
