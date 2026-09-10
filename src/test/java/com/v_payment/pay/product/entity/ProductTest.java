package com.v_payment.pay.product.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @DisplayName("상품 생성 시 주어진 값이 제대로 들어간다.")
    @Test
    void create() {
        // given
        String name = "product";
        Long price = 10_000L;
        Integer stockQuantity = 100;

        // when
        Product product = Product.create(name, price, stockQuantity);

        // then
        assertThat(product.getName()).isEqualTo(name);
        assertThat(product.getPrice()).isEqualTo(price);
        assertThat(product.getStockQuantity()).isEqualTo(stockQuantity);
    }

    @DisplayName("상품명은 필수다")
    @Test
    void createWithNullName() {
        assertThatThrownBy(() -> Product.create(null, 10_000L, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("상품명은 비어 있을 수 없다")
    @Test
    void createWithBlankName() {
        assertThatThrownBy(() -> Product.create(" ", 10_000L, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("상품 가격은 필수다")
    @Test
    void createWithNullPrice() {
        assertThatThrownBy(() -> Product.create("product", null, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("상품 가격은 음수일 수 없다")
    @Test
    void createWithNegativePrice() {
        assertThatThrownBy(() -> Product.create("product", -1L, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("상품 재고는 필수다")
    @Test
    void createWithNullStockQuantity() {
        assertThatThrownBy(() -> Product.create("product", 10_000L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("상품 재고는 음수일 수 없다")
    @Test
    void createWithNegativeStockQuantity() {
        assertThatThrownBy(() -> Product.create("product", 10_000L, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
