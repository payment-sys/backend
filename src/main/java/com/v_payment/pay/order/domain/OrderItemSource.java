package com.v_payment.pay.order.domain;

import lombok.Getter;

@Getter
public class OrderItemSource {
    private final Long productId;
    private final String name;
    private final Long price;
    private final Integer quantity;

    private OrderItemSource(Long productId, String name, Long price, Integer quantity) {
        this.productId = validateProductId(productId);
        this.name = validateName(name);
        this.price = validatePrice(price);
        this.quantity = validateQuantity(quantity);
    }

    public static OrderItemSource create(Long productId, String name, Long price, int quantity) {
        return new OrderItemSource(productId, name, price, quantity);
    }

    private Long validateProductId(Long productId) {
        if (productId == null) throw new IllegalArgumentException("상품 ID는 필수입니다.");
        return productId;
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("상품명은 필수입니다.");
        return name;
    }

    private Long validatePrice(Long price) {
        if (price == null) throw new IllegalArgumentException("상품 가격은 필수입니다.");
        if (price < 0) throw new IllegalArgumentException("상품 가격은 음수일 수 없습니다.");
        return price;
    }

    private int validateQuantity(int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("주문 수량은 음수일 수 없습니다..");
        return quantity;
    }
}
