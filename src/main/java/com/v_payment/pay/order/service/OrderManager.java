package com.v_payment.pay.order.service;

import com.v_payment.pay.global.exception.BusinessException;
import com.v_payment.pay.order.entity.OrderStatus;
import com.v_payment.pay.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.v_payment.pay.order.exception.OrderException.ORDER_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class OrderManager {
    private final OrderRepository orderRepository;

    public void markPaid(String orderCode) {
        int updatedRows = orderRepository.markPaid(
                orderCode,
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.PAYMENT_FAILED,
                OrderStatus.PAID
        );
        validateOrderUpdatedRows(updatedRows);
    }

    public void markPaymentFailed(String orderCode) {
        int updatedRows = orderRepository.markPaymentFailed(
                orderCode,
                OrderStatus.PENDING_PAYMENT,
                OrderStatus.PAYMENT_FAILED
        );
        validateOrderUpdatedRows(updatedRows);
    }

    private void validateOrderUpdatedRows(int updatedRows) {
        if (updatedRows != 1) throw new BusinessException(ORDER_NOT_FOUND);
    }
}
