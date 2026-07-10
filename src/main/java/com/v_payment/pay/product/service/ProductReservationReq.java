package com.v_payment.pay.product.service;

public record ProductReservationReq(
        Long productId,
        Integer quantity
) {
}
