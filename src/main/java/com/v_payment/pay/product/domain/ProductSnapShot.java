package com.v_payment.pay.product.domain;

import com.v_payment.pay.product.domain.entity.Product;

public class ProductSnapShot {
    private final Long productId;
    private final Integer quantity;
    private Integer changeCount;

    private ProductSnapShot(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
        this.changeCount = 0;
    }

    public static ProductSnapShot from(Product product) {
        return new ProductSnapShot(product.getId(), product.getStockQuantity());
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getStockBefore() {
        return quantity + changeCount;
    }

    public Integer getStockAfter(Integer quantityToChange) {
        return getStockBefore() + quantityToChange;
    }

    public boolean canChange(Integer quantityToChange) {
        return getStockAfter(quantityToChange) >= 0;
    }

    public void change(Integer quantityToChange) {
        this.changeCount += quantityToChange;
    }
}
