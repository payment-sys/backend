package com.v_payment.pay.order.service;

import com.v_payment.pay.order.entity.OrderItem;
import com.v_payment.pay.order.entity.OrderStatus;
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

    public OrderStatusUpdateResult updateStatus(String orderCode, OrderStatus orderStatus) {
        int updatedRows = orderRepository.updateStatus(
                orderCode,
                List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PRODUCT_RESERVED),
                orderStatus
        );
        if (updatedRows != 1) {
            return OrderStatusUpdateResult.notUpdated();
        }

        if (orderStatus == OrderStatus.PAID) {
            return OrderStatusUpdateResult.updated(List.of());
        }

        return OrderStatusUpdateResult.updated(orderItemRepository.findAllByOrderCode(orderCode).stream()
                .map(OrderItemSnapshot::from)
                .toList());
    }

    public ProductQuantityReservationUpdateResult updateProductQuantityReservationStatus(
            Collection<String> successOrderCodes,
            Collection<String> failOrderCodes
    ) {
        int successUpdatedRows = updateProductReserved(successOrderCodes);
        int failUpdatedRows = updateProductReservationFailed(failOrderCodes);

        return new ProductQuantityReservationUpdateResult(successUpdatedRows, failUpdatedRows);
    }

    private int updateProductReserved(Collection<String> orderCodes) {
        if (orderCodes.isEmpty()) {
            return 0;
        }

        return orderRepository.updateStatusByOrderCodes(
                orderCodes,
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.PRODUCT_RESERVED
        );
    }

    private int updateProductReservationFailed(Collection<String> orderCodes) {
        if (orderCodes.isEmpty()) {
            return 0;
        }

        return orderRepository.updateStatusByOrderCodes(
                orderCodes,
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.PAYMENT_FAILED
        );
    }

    public record ProductQuantityReservationUpdateResult(
            int successUpdatedRows,
            int failUpdatedRows
    ) {
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
