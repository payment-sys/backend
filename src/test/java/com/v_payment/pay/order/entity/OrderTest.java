package com.v_payment.pay.order.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @DisplayName("주문을 처음 생성 시 기본 상태를 갖는다.")
    @Test
    void create() {
        // given
        String orderCode = "ORDER-001";
        LocalDateTime orderedAt = LocalDateTime.of(2026, 8, 31, 10, 0);

        // when
        Order order = Order.create(orderCode, orderedAt);

        // then
        assertThat(order.getOrderCode()).isEqualTo(orderCode);
        assertThat(order.isFailed()).isFalse();
        assertThat(order.getTotalAmount()).isEqualTo(0L);
        assertThat(order.getOrderedAt()).isEqualTo(orderedAt);
        assertThat(order.getOrderItems()).isEmpty();
    }

    @DisplayName("주문 상품 추가 시 주문 상품 목록에 추가되고, 총 금액(합산)이 증가한다.")
    @Test
    void addItem() {
        // given
        Order order = Order.create("ORDER-001", LocalDateTime.of(2026, 8, 31, 10, 0));

        Long productId = 1L;
        String productName = "테스트 상품";
        Long unitPrice = 10_000L;
        Integer quantity = 2;

        // when
        order.addItem(productId, productName, unitPrice, quantity);

        // then
        assertThat(order.getOrderItems()).hasSize(1);
        assertThat(order.getTotalAmount()).isEqualTo(20_000L);

        OrderItem orderItem = order.getOrderItems().get(0);
        assertThat(orderItem.getOrder()).isSameAs(order);
        assertThat(orderItem.getProductId()).isEqualTo(productId);
        assertThat(orderItem.getProductName()).isEqualTo(productName);
        assertThat(orderItem.getUnitPrice()).isEqualTo(unitPrice);
        assertThat(orderItem.getQuantity()).isEqualTo(quantity);
        assertThat(orderItem.getOrderAmount()).isEqualTo(20_000L);
    }

    @DisplayName("주문 상품을 여러 개 추가하면 총 금액이 누적된다")
    @Test
    void addItems() {
        // given
        Order order = Order.create("ORDER-001", LocalDateTime.of(2026, 8, 31, 10, 0));

        // when
        order.addItem(1L, "상품 A", 10_000L, 2);
        order.addItem(2L, "상품 B", 5_000L, 3);

        // then
        assertThat(order.getOrderItems()).hasSize(2);
        assertThat(order.getTotalAmount()).isEqualTo(35_000L);
    }
}