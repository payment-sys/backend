package com.v_payment.pay.order.entity;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemInfo {
    private Long productId;

    private String productName;

    private Long unitPrice;

    private Integer quantity;

    private OrderItemInfo(Long productId, String productName, Long unitPrice, Integer quantity) {
        this.productId = validateProductId(productId);
        this.productName = validateProductName(productName);
        this.unitPrice = validateUnitPrice(unitPrice);
        this.quantity = validateQuantity(quantity);
    }

    public Long calculateOrderAmount() {
        return unitPrice * quantity;
    }

    public static OrderItemInfo create(Long productId, String productName, Long unitPrice, Integer quantity) {
        return new OrderItemInfo(productId, productName, unitPrice, quantity);
    }

    private Long validateProductId(Long productId) {
        if (productId == null) throw new IllegalArgumentException("productId null일 수 없습니다!");
        return productId;
    }

    private String validateProductName(String productName) {
        if (productName == null || productName.isBlank()) throw new IllegalArgumentException("productName은 필수입니다!");
        return productName;
    }

    private Long validateUnitPrice(Long unitPrice) {
        if (unitPrice == null) throw new IllegalArgumentException("unitPrice null일 수 없습니다!");
        if (unitPrice < 0) throw new IllegalArgumentException("unitPrice는 음수일 수 없습니다!");
        return unitPrice;
    }

    private Integer validateQuantity(Integer quantity) {
        if (quantity == null) throw new IllegalArgumentException("quantity는 null일 수 없습니다!");
        if (quantity <= 0) throw new IllegalArgumentException("quantity는 0이거나 음수일 수 없습니다!");
        return quantity;
    }
}
