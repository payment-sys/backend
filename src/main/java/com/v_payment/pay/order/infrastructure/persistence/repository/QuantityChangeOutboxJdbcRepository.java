package com.v_payment.pay.order.infrastructure.persistence.repository;

import com.v_payment.pay.order.domain.outbox.QuantityChangeOutbox;

import java.util.List;

public interface QuantityChangeOutboxJdbcRepository {
    List<QuantityChangeOutbox> createOutboxBatch(List<QuantityChangeOutbox> outboxes);
}
