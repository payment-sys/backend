package com.v_payment.pay.order.entity;

import com.v_payment.pay.order.controller.dto.req.OrderItemCreateReq;
import com.v_payment.pay.order.domain.OrderItemSources;
import com.v_payment.pay.order.domain.ReqQuantities;
import com.v_payment.pay.order.domain.entity.Order;
import com.v_payment.pay.order.domain.entity.OrderItem;
import com.v_payment.pay.order.domain.entity.OrderStatus;
import com.v_payment.pay.product.domain.ProductBasicInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @DisplayName("주문을 생성하면 기본 상태를 가진다")
    @Test
    void create() {
        // given
        String orderCode = "ORDER-001";
        LocalDateTime orderedAt = LocalDateTime.of(2026, 8, 31, 10, 0);

        // when
        Order order = Order.create(orderCode, orderedAt);

        // then
        assertThat(order.getOrderCode()).isEqualTo(orderCode);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getTotalAmount()).isEqualTo(0L);
        assertThat(order.getOrderedAt()).isEqualTo(orderedAt);
        assertThat(order.getOrderItems()).isEmpty();
    }

    @DisplayName("주문 상품을 추가하면 주문 상품 목록과 총 금액이 갱신된다")
    @Test
    void addItem() {
        // given
        Order order = Order.create("ORDER-001", LocalDateTime.of(2026, 8, 31, 10, 0));

        Long productId = 1L;
        String productName = "테스트 상품";
        Long unitPrice = 10_000L;
        Integer quantity = 2;

        // when
        order.addItems(orderItemSources(
                List.of(new ProductBasicInfo(productId, productName, unitPrice)),
                List.of(new OrderItemCreateReq(productId, quantity))
        ));

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

    @DisplayName("여러 주문 상품을 추가하면 총 금액을 누적한다")
    @Test
    void addItems() {
        // given
        Order order = Order.create("ORDER-001", LocalDateTime.of(2026, 8, 31, 10, 0));

        // when
        order.addItems(orderItemSources(
                List.of(
                        new ProductBasicInfo(1L, "상품 A", 10_000L),
                        new ProductBasicInfo(2L, "상품 B", 5_000L)
                ),
                List.of(
                        new OrderItemCreateReq(1L, 2),
                        new OrderItemCreateReq(2L, 3)
                )
        ));

        // then
        assertThat(order.getOrderItems()).hasSize(2);
        assertThat(order.getTotalAmount()).isEqualTo(35_000L);
    }

    private OrderItemSources orderItemSources(
            List<ProductBasicInfo> productBasicInfos,
            List<OrderItemCreateReq> reqs
    ) {
        return OrderItemSources.of(productBasicInfos, ReqQuantities.from(reqs));
    }
}
