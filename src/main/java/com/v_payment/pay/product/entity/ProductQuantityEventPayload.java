package com.v_payment.pay.product.entity;

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

    private Map<String, Integer> requestedQuantities;

    public static ProductQuantityEventPayload of(String orderCode, PaymentMethod paymentMethod,
                                                 Map<Long, Integer> requestedQuantities) {
        Map<String, Integer> stringKeyRequestedQuantities = requestedQuantities.entrySet().stream()
                .collect(Collectors.toMap(entry -> String.valueOf(entry.getKey()), Map.Entry::getValue));

        return new ProductQuantityEventPayload(orderCode, paymentMethod, stringKeyRequestedQuantities);
    }
}
