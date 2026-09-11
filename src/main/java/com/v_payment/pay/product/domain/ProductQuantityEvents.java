package com.v_payment.pay.product.domain;


import com.v_payment.pay.product.domain.entity.ProductQuantityEvent;
import com.v_payment.pay.product.domain.entity.ProductQuantityEventPayload;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ProductQuantityEvents {
    private final List<ProductQuantityEvent> productQuantityEvents;

    public boolean isEmptyEvent() {
        return productQuantityEvents.isEmpty();
    }

    public List<Long> getIds() {
        return productQuantityEvents.stream().map(ProductQuantityEvent::getId).toList();
    }

    public List<ProductQuantityEventPayload> getPayloads() {
        return productQuantityEvents.stream().map(ProductQuantityEvent::getPayload).toList();
    }

    public List<ProductQuantityEvent> getEvents() {
        return productQuantityEvents;
    }

    public List<Long> getProductIdsDistinct() {
        return getPayloads().stream()
                .flatMap(payload -> payload.getRequestedQuantities().keySet().stream())
                .distinct()
                .toList();
    }

    public static ProductQuantityEvents from(List<ProductQuantityEvent> productQuantityEvents) {
        return new ProductQuantityEvents(productQuantityEvents);
    }
}
