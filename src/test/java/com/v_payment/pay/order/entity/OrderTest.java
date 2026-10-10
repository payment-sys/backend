package com.v_payment.pay.order.entity;

import com.v_payment.pay.order.domain.order.Order;
import com.v_payment.pay.order.domain.order.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @DisplayName("order is created with default state")
    @Test
    void create() {
        String orderCode = "ORDER-001";
        LocalDateTime orderedAt = LocalDateTime.of(2026, 8, 31, 10, 0);

        Order order = Order.create(orderCode, orderedAt);

        assertThat(order.getOrderCode()).isEqualTo(orderCode);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getTotalAmount()).isEqualTo(0L);
        assertThat(order.getOrderedAt()).isEqualTo(orderedAt);
        assertThat(order.getOrderItemInfos()).isEmpty();
    }
}
