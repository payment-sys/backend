package com.v_payment.pay.product.domain.event;

import com.v_payment.pay.product.domain.entity.QuantityChangeResultOutbox;

import java.util.List;

public record QuantityChangeResultOutboxesEvent(
        List<QuantityChangeResultOutbox> outboxes
) {
    public QuantityChangeResultOutboxesEvent {
        if (outboxes == null || outboxes.isEmpty()) {
            throw new IllegalArgumentException("outboxes are required.");
        }
        outboxes = List.copyOf(outboxes);
    }
}
