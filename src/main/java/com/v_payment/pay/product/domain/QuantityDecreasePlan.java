package com.v_payment.pay.product.domain;

import com.v_payment.pay.product.domain.entity.ProductQuantityEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuantityDecreasePlan {
    private final List<ProductQuantityEvent> success = new ArrayList<>();
    private final List<ProductQuantityEvent> failure = new ArrayList<>();
    private final Map<Long, Integer> decreaseTotal = new HashMap<>();

    public void addSuccessEvent(ProductQuantityEvent event) {
        success.add(event);
    }

    public void addFailureEvent(ProductQuantityEvent event) {
        failure.add(event);
    }

    public void recordDecreaseSimulation(Long productId, int quantity) {
        decreaseTotal.merge(productId, -quantity, Integer::sum);
    }

    public List<ProductQuantityEvent> success() {
        return success;
    }

    public List<ProductQuantityEvent> failure() {
        return failure;
    }

    public boolean hasFailOrder() {
        return !failure.isEmpty();
    }

    public List<String> getFailOrderCodes() {
        return failure.stream().map(ProductQuantityEvent::getOrderCode).toList();
    }

    public boolean hasDecreaseTotal() {
        return !decreaseTotal.isEmpty();
    }

    public Map<Long, Integer> getDecreaseTotal() {
        return Map.copyOf(decreaseTotal);
    }

    public List<ProductQuantityEvent> getSuccess() {
        return List.copyOf(success);
    }
}
