package com.v_payment.pay.order.domain.order;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemInfo {
    private Long productId;

    private String productName;

    private Long unitPrice;

    private Integer quantity;

    private OrderItemStatus status;

    private OrderItemInfo(Long productId, String productName, Long unitPrice, Integer quantity, OrderItemStatus status) {
        this.productId = validateProductId(productId);
        this.productName = validateProductName(productName);
        this.unitPrice = validateUnitPrice(unitPrice);
        this.quantity = validateQuantity(quantity);
        this.status = validateStatus(status);
    }

    public Long calculateOrderAmount() {
        return unitPrice * quantity;
    }

    public static OrderItemInfo create(Long productId, String productName, Long unitPrice, Integer quantity) {
        return new OrderItemInfo(productId, productName, unitPrice, quantity, OrderItemStatus.PROCESSING);
    }

    public static OrderItemInfo create(Long productId, String productName, Long unitPrice, Integer quantity, OrderItemStatus status) {
        return new OrderItemInfo(productId, productName, unitPrice, quantity, status);
    }

    private Long validateProductId(Long productId) {
        if (productId == null) throw new IllegalArgumentException("productId는 필수입니다.");
        return productId;
    }

    private String validateProductName(String productName) {
        if (productName == null || productName.isBlank()) throw new IllegalArgumentException("productName은 필수입니다.");
        return productName;
    }

    private Long validateUnitPrice(Long unitPrice) {
        if (unitPrice == null) throw new IllegalArgumentException("unitPrice는 필수입니다.");
        if (unitPrice < 0) throw new IllegalArgumentException("unitPrice는 음수일 수 없습니다.");
        return unitPrice;
    }

    private Integer validateQuantity(Integer quantity) {
        if (quantity == null) throw new IllegalArgumentException("quantity는 필수입니다.");
        if (quantity <= 0) throw new IllegalArgumentException("quantity는 0보다 커야 합니다.");
        return quantity;
    }
    private OrderItemStatus validateStatus(OrderItemStatus status) {
        if (status == null) throw new IllegalArgumentException("status is required.");
        return status;
    }
}
