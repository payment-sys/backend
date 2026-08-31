package com.v_payment.pay.product.entity;

import com.v_payment.pay.payment.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductQuantityEventPayload {
    private String orderCode;

    private PaymentMethod paymentMethod;

    @JsonDeserialize(keyUsing = LongMapKeyDeserializer.class)
    private Map<Long, Integer> requestedQuantities;

    public static ProductQuantityEventPayload of(String orderCode, PaymentMethod paymentMethod,
                                                 Map<Long, Integer> requestedQuantities) {
        return new ProductQuantityEventPayload(orderCode, paymentMethod, requestedQuantities);
    }
}
