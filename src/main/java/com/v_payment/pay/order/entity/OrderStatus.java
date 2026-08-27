package com.v_payment.pay.order.entity;

public enum OrderStatus {
    PENDING,
    PRODUCT_RESERVED_SUCCESS,
    PRODUCT_RESERVED_FAILED,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    CANCELLED
}
