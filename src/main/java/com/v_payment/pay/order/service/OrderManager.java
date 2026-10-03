package com.v_payment.pay.order.service;

import com.v_payment.pay.order.domain.orderitem.OrderItem;
import com.v_payment.pay.order.domain.orderitem.OrderItemInfo;
import com.v_payment.pay.order.domain.order.OrderStatus;
import com.v_payment.pay.order.repository.OrderItemRepository;
import com.v_payment.pay.order.repository.OrderRepository;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderManager {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public boolean markCreated(String orderCode) {
        return markStatus(orderCode, OrderStatus.CREATED, OrderStatus.CREATED);
    }

    public boolean markCreated(Collection<String> orderCodes) {
        return markStatus(orderCodes, OrderStatus.CREATED, OrderStatus.CREATED);
    }

    public boolean markLackQuantity(String orderCode) {
        return markStatus(orderCode, OrderStatus.CREATED, OrderStatus.LACK_QUANTITY);
    }

    public boolean markLackQuantity(Collection<String> orderCodes) {
        return markStatus(orderCodes, OrderStatus.CREATED, OrderStatus.LACK_QUANTITY);
    }

    public boolean markPaid(String orderCode) {
        return markStatus(orderCode, OrderStatus.ORDER_SUCCESS, OrderStatus.PAID);
    }

    public boolean markPaid(Collection<String> orderCodes) {
        return markStatus(orderCodes, OrderStatus.ORDER_SUCCESS, OrderStatus.PAID);
    }

    public boolean markExpired(String orderCode) {
        return markStatus(orderCode, OrderStatus.CREATED, OrderStatus.EXPIRED);
    }

    public boolean markExpired(Collection<String> orderCodes) {
        return markStatus(orderCodes, OrderStatus.CREATED, OrderStatus.EXPIRED);
    }

    public boolean markLackQuantities(String orderCode) {
        return markLackQuantity(orderCode);
    }

    @WithSpan("order.OrderManager.markLackQuantities")
    public boolean markLackQuantities(Collection<String> orderCodes) {
        return markLackQuantity(orderCodes);
    }

    public boolean isCreatedStatus(String orderCode) {
        return orderRepository.existsByOrderCodeAndStatus(orderCode, OrderStatus.CREATED);
    }

    public List<OrderItemInfo> findOrderItems(String orderCode) {
        return orderItemRepository.findAllByOrderCode(orderCode).stream()
                .map(OrderItem::getOrderItemInfo)
                .toList();
    }

    private boolean markStatus(String orderCode, OrderStatus currentStatus, OrderStatus targetStatus) {
        return orderRepository.markStatus(orderCode, currentStatus, targetStatus) == 1;
    }

    private boolean markStatus(Collection<String> orderCodes, OrderStatus currentStatus, OrderStatus targetStatus) {
        if (orderCodes.isEmpty()) return true;
        return orderRepository.markStatusByOrderCodes(orderCodes, currentStatus, targetStatus) == orderCodes.size();
    }
}
