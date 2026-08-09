package com.v_payment.pay.product.cache.dto;

public record CachedProduct(
        Long productId,
        String productName,
        Long price,
        int quantity
) {
    public CachedProduct changeQuantity(int quantity) {
        return new CachedProduct(
                this.productId,
                this.productName,
                this.price,
                this.quantity + quantity);
    }
}
