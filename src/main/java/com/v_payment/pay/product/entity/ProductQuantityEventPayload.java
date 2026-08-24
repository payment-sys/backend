package com.v_payment.pay.product.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductQuantityEventPayload {
    private Map<Long, Integer> requestedQuantities;
}
