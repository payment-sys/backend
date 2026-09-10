package com.v_payment.pay.product.exception;

import com.v_payment.pay.product.entity.ProductQuantityEvent;

import java.util.List;

public class ProductQuantityEventConsumeException extends RuntimeException {
    private final List<Long> eventIds;
    private final List<ProductQuantityEvent> events;

    public ProductQuantityEventConsumeException(
            List<Long> eventIds,
            List<ProductQuantityEvent> events,
            Throwable cause
    ) {
        super("상품 수량 이벤트 소비 중 오류가 발생했습니다.", cause);
        this.eventIds = List.copyOf(eventIds);
        this.events = List.copyOf(events);
    }

    public List<Long> getEventIds() {
        return eventIds;
    }

    public List<ProductQuantityEvent> getEvents() {
        return events;
    }
}
