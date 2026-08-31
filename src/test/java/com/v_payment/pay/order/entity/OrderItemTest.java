package com.v_payment.pay.order.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemTest {

    @DisplayName("주문 상품 생성할 수 있다.")
    @Test
    void create() {
        // given
        Order order = Order.create("ORDER-001", LocalDateTime.of(2026, 8, 31, 10, 0));
        Long productId = 1L;
        String productName = "product";
        Long unitPrice = 10_000L;
        Integer quantity = 2;

        // when
        OrderItem orderItem = OrderItem.create(order, productId, productName, unitPrice, quantity);

        // then
        assertThat(orderItem.getOrder()).isSameAs(order);
        assertThat(orderItem.getProductId()).isEqualTo(productId);
        assertThat(orderItem.getProductName()).isEqualTo(productName);
        assertThat(orderItem.getUnitPrice()).isEqualTo(unitPrice);
        assertThat(orderItem.getQuantity()).isEqualTo(quantity);
        assertThat(orderItem.getOrderAmount()).isEqualTo(20_000L);
    }

    @DisplayName("주문상품은 방어적 복사가 된다.")
    @Test
    void getOrderItemInfo() {
        // given
        Order order = Order.create("ORDER-001", LocalDateTime.of(2026, 8, 31, 10, 0));
        OrderItem orderItem = OrderItem.create(order, 1L, "product", 10_000L, 2);

        // when
        OrderItemInfo orderItemInfo = orderItem.getOrderItemInfo();

        // then
        assertThat(orderItemInfo).isNotSameAs(orderItem.getItemInfo());
        assertThat(orderItemInfo.getProductId()).isEqualTo(orderItem.getProductId());
        assertThat(orderItemInfo.getProductName()).isEqualTo(orderItem.getProductName());
        assertThat(orderItemInfo.getUnitPrice()).isEqualTo(orderItem.getUnitPrice());
        assertThat(orderItemInfo.getQuantity()).isEqualTo(orderItem.getQuantity());
    }
}
