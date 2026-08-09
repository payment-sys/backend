package com.v_payment.pay.product.controller.dto.res;

public record ReservedProduct(
        Long productId,
        String productName,
        Long unitPrice,
        Integer quantity
) {
    public static ReservedProduct of(Long id, String name, Long price, int quantity) {
        return new ReservedProduct(id, name, price, quantity);
    }
}
