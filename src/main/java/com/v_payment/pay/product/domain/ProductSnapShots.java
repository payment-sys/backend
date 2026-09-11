package com.v_payment.pay.product.domain;

import com.v_payment.pay.product.domain.entity.Product;
import com.v_payment.pay.product.domain.entity.ProductQuantityEvent;

import java.util.Map;
import java.util.stream.Collectors;

public class ProductSnapShots {
    private final Map<Long, ProductSnapShot> productSnapshots;

    public ProductSnapShots(Map<Long, ProductSnapShot> productSnapshots) {
        this.productSnapshots = productSnapshots;
    }

    public boolean isEventSuccess(ProductQuantityEvent productQuantityEvent) {
        return productQuantityEvent.canMatchCondition(this::canDecrease);
    }

    public void recordDecreaseSimulation(ProductQuantityEvent productQuantityEvent, QuantityDecreasePlan plan) {
        productQuantityEvent.getPayload().getRequestedQuantities().forEach((productId, quantity) -> {
            ProductSnapShot productSnapShot = productSnapshots.get(productId);
            productSnapShot.decrease(quantity);
            plan.recordDecreaseSimulation(productId, quantity);
        });
    }

    private boolean canDecrease(Long productId, int quantity) {
        ProductSnapShot productSnapShot = productSnapshots.get(productId);
        return productSnapShot != null && productSnapShot.canDecrease(quantity);
    }

    public static ProductSnapShots from(Map<Long, Product> products) {
        return new ProductSnapShots(products.values().stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        product -> new ProductSnapShot(product.getStockQuantity()))));
    }
}
