package com.v_payment.pay.product.controller.dto.res;

import com.v_payment.pay.product.entity.Product;

public record ProductCreateRes(
        Long productId,
        String name,
        Long price,
        Integer stockQuantity
) {
    public static ProductCreateRes from(Product product) {
        return new ProductCreateRes(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStockQuantity()
        );
    }
}
