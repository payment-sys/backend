package com.v_payment.pay.product.repository;

import com.v_payment.pay.product.domain.entity.QuantityChangeResultOutbox;
import com.v_payment.pay.product.domain.entity.QuantityChangeResultOutboxStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface QuantityChangeResultOutboxJdbcRepository {
    List<QuantityChangeResultOutbox> createOutboxBatch(List<QuantityChangeResultOutbox> outboxes);

    void markDoneBatch(
            List<QuantityChangeResultOutbox> outboxes,
            QuantityChangeResultOutboxStatus readyStatus,
            QuantityChangeResultOutboxStatus doneStatus,
            LocalDateTime updatedAt
    );
}
