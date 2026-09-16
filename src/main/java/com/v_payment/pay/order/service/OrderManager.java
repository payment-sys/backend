package com.v_payment.pay.order.service;

import com.v_payment.pay.global.exception.BusinessException;
import com.v_payment.pay.order.domain.entity.Order;
import com.v_payment.pay.order.domain.entity.OrderItem;
import com.v_payment.pay.order.domain.entity.OrderItemInfo;
import com.v_payment.pay.order.domain.entity.OrderStatus;
import com.v_payment.pay.order.exception.OrderException;
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
        return orderRepository.markFailed(orderCode, OrderStatus.CREATED, OrderStatus.LACK_QUANTITY) == 1;
    }

    public boolean markFailed(Collection<String> orderCodes) {
        if (orderCodes.isEmpty()) return true;
        return orderRepository.markFailedByOrderCodes(orderCodes, OrderStatus.CREATED, OrderStatus.LACK_QUANTITY) == orderCodes.size();
    }

    public List<OrderItemInfo> findOrderItems(String orderCode) {
        return orderItemRepository.findAllByOrderCode(orderCode).stream()
                .map(OrderItem::getOrderItemInfo)
                .toList();
    }
}
