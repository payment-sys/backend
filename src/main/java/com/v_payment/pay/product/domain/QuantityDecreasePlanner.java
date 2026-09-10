package com.v_payment.pay.product.domain;

import com.v_payment.pay.product.domain.entity.ProductQuantityEvent;

import java.util.Map;

public class QuantityDecreasePlanner {
    private final ProductSnapShots productSnapShots;

    private QuantityDecreasePlanner(ProductSnapShots productSnapShots) {
        this.productSnapShots = productSnapShots;
    }

    public QuantityDecreasePlan makeDecreasePlan(ProductQuantityEvents events) {
        QuantityDecreasePlan quantityDecreasePlan = new QuantityDecreasePlan();
        for(ProductQuantityEvent event : events.getEvents()) {
            if(productSnapShots.isEventSuccess(event)) {
                productSnapShots.recordDecreaseSimulation(event, quantityDecreasePlan);
                quantityDecreasePlan.addSuccessEvent(event);
                continue;
            }
            quantityDecreasePlan.addFailureEvent(event);
        }
        return quantityDecreasePlan;
    }

    public static QuantityDecreasePlanner from(ProductSnapShots productSnapShots) {
        return new QuantityDecreasePlanner(productSnapShots);
    }
}
