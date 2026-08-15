package com.v_payment.pay.product.cache.dto;

public record WriteAheadLogItem(
        Long productId,
        Integer delta
) {
}
