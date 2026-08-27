package com.v_payment.pay.product.entity;

import com.v_payment.pay.order.controller.dto.req.OrderCreateReq;
import com.v_payment.pay.order.controller.dto.req.OrderItemCreateReq;
import com.v_payment.pay.payment.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductQuantityEventPayload {
    private String orderCode;

    private PaymentMethod paymentMethod;

    private Map<Long, Integer> requestedQuantities;

    public static ProductQuantityEventPayload of(String orderCode, OrderCreateReq orderCreateReq) {
        return new ProductQuantityEventPayload(
                orderCode,
                orderCreateReq.paymentMethod(),
                orderCreateReq.items().stream()
                        .collect(Collectors.toMap(OrderItemCreateReq::productId, OrderItemCreateReq::quantity))
        );
    }
}
