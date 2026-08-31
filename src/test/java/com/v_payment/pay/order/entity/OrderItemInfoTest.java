package com.v_payment.pay.order.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderItemInfoTest {

    @DisplayName("주문 상품 정보는 상품 스냅샷 값으로 생성된다")
    @Test
    void create() {
        // given
        Long productId = 1L;
        String productName = "product";
        Long unitPrice = 10_000L;
        Integer quantity = 2;

        // when
        OrderItemInfo orderItemInfo = OrderItemInfo.create(productId, productName, unitPrice, quantity);

        // then
        assertThat(orderItemInfo.getProductId()).isEqualTo(productId);
        assertThat(orderItemInfo.getProductName()).isEqualTo(productName);
        assertThat(orderItemInfo.getUnitPrice()).isEqualTo(unitPrice);
        assertThat(orderItemInfo.getQuantity()).isEqualTo(quantity);
    }

    @DisplayName("주문 상품 정보는 주문 금액을 계산한다")
    @Test
    void calculateOrderAmount() {
        // given
        OrderItemInfo orderItemInfo = OrderItemInfo.create(1L, "product", 10_000L, 2);

        // when
        Long orderAmount = orderItemInfo.calculateOrderAmount();

        // then
        assertThat(orderAmount).isEqualTo(20_000L);
    }

    @DisplayName("상품 ID는 필수다")
    @Test
    void createWithNullProductId() {
        assertThatThrownBy(() -> OrderItemInfo.create(null, "product", 10_000L, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("상품명은 비어 있을 수 없다")
    @Test
    void createWithBlankProductName() {
        assertThatThrownBy(() -> OrderItemInfo.create(1L, " ", 10_000L, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("상품 단가는 필수다")
    @Test
    void createWithNullUnitPrice() {
        assertThatThrownBy(() -> OrderItemInfo.create(1L, "product", null, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("상품 단가는 음수일 수 없다")
    @Test
    void createWithNegativeUnitPrice() {
        assertThatThrownBy(() -> OrderItemInfo.create(1L, "product", -1L, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("수량은 필수다")
    @Test
    void createWithNullQuantity() {
        assertThatThrownBy(() -> OrderItemInfo.create(1L, "product", 10_000L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("수량은 양수여야 한다")
    @Test
    void createWithZeroQuantity() {
        assertThatThrownBy(() -> OrderItemInfo.create(1L, "product", 10_000L, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
