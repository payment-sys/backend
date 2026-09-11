package com.v_payment.pay.product.domain;

public record ProductBasicInfo(
        Long productId,
        String name,
        Long price
) {
}
