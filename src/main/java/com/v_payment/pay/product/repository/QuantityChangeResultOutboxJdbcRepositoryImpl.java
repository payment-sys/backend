package com.v_payment.pay.product.repository;

import com.v_payment.pay.product.domain.entity.QuantityChangeResultOutbox;
import com.v_payment.pay.product.domain.entity.QuantityChangeResultOutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class QuantityChangeResultOutboxJdbcRepositoryImpl implements QuantityChangeResultOutboxJdbcRepository {
    private static final String OUTBOX_BATCH = """
                insert into quantity_change_result_outbox (
                    order_code,
                    product_id,
                    change_count,
                    change_status,
                    fail_reason,
                    status,
                    retry_count,
                    next_attempt_time,
                    created_at,
                    updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

    private static final String MARK_DONE_BATCH = """
                update quantity_change_result_outbox
                set status = ?,
                    updated_at = ?
                where order_code = ?
                  and product_id = ?
                  and status = ?
                """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void createOutboxBatch(List<QuantityChangeResultOutbox> outboxes) {
            jdbcTemplate.batchUpdate(OUTBOX_BATCH, outboxes, 100, (ps, outbox) -> {
                ps.setString(1, outbox.getOrderCode());
                ps.setLong(2, outbox.getProductId());
                ps.setInt(3, outbox.getChangeCount());
                ps.setString(4, outbox.getChangeStatus().name());
                ps.setString(5, outbox.getFailReason());
                ps.setString(6, outbox.getQuantityChangeResultOutboxStatus().name());
                ps.setInt(7, outbox.getRetryCount());
                ps.setObject(8, outbox.getNextAttemptTime());
                ps.setObject(9, outbox.getCreatedAt());
                ps.setObject(10, outbox.getUpdatedAt());
            });
    }

    @Override
    public void markDoneBatch(
            List<QuantityChangeResultOutbox> outboxes,
            QuantityChangeResultOutboxStatus readyStatus,
            QuantityChangeResultOutboxStatus doneStatus,
            LocalDateTime updatedAt
    ) {
        jdbcTemplate.batchUpdate(MARK_DONE_BATCH, outboxes, 100, (ps, outbox) -> {
            ps.setString(1, doneStatus.name());
            ps.setObject(2, updatedAt);
            ps.setString(3, outbox.getOrderCode());
            ps.setLong(4, outbox.getProductId());
            ps.setString(5, readyStatus.name());
        });
    }
}
