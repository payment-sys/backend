package com.v_payment.pay.order.entrypoint.dto.res;

public record OrderCreateRes(
        String orderCode
) {
    public static OrderCreateRes from(String orderCode) {
        return new OrderCreateRes(orderCode);
    }
}
