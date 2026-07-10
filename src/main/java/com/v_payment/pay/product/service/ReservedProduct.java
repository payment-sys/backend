package com.v_payment.pay.product.service;

import com.v_payment.pay.product.entity.Product;

public record ReservedProduct(
        Long productId,
        String productName,
        Long unitPrice,
        Integer quantity
) {
    public static ReservedProduct of(Product product, Integer quantity) {
        return new ReservedProduct(product.getId(), product.getName(), product.getPrice(), quantity);
    }
}
