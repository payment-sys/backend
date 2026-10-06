package com.v_payment.pay.product.repository;

import com.v_payment.pay.product.domain.entity.QuantityChangeResultOutbox;
import com.v_payment.pay.product.domain.entity.QuantityChangeResultOutboxStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class QuantityChangeResultOutboxJdbcRepositoryImpl implements QuantityChangeResultOutboxJdbcRepository {
    private static final String OUTBOX_BATCH = """
                insert ignore into quantity_change_result_outbox (
                    order_code,
                    product_id,
                    change_count,
                    products_count,
                    change_status,
                    fail_reason,
                    status,
                    retry_count,
                    next_attempt_time,
                    created_at,
                    updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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

    public QuantityChangeResultOutboxJdbcRepositoryImpl(
            @Qualifier("productJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<QuantityChangeResultOutbox> createOutboxBatch(List<QuantityChangeResultOutbox> outboxes) {
        int[][] updateCounts = jdbcTemplate.batchUpdate(OUTBOX_BATCH, outboxes, 100, (ps, outbox) -> {
            ps.setString(1, outbox.getOrderCode());
            ps.setLong(2, outbox.getProductId());
            ps.setInt(3, outbox.getChangeCount());
            ps.setInt(4, outbox.getProductsCount());
            ps.setString(5, outbox.getChangeStatus().name());
            ps.setString(6, outbox.getFailReason());
            ps.setString(7, outbox.getQuantityChangeResultOutboxStatus().name());
            ps.setInt(8, outbox.getRetryCount());
            ps.setObject(9, outbox.getNextAttemptTime());
            ps.setObject(10, outbox.getCreatedAt());
            ps.setObject(11, outbox.getUpdatedAt());
        });

        List<QuantityChangeResultOutbox> insertedOutboxes = new ArrayList<>();
        int outboxIndex = 0;
        for (int[] batchUpdateCounts : updateCounts) {
            for (int updateCount : batchUpdateCounts) {
                if (updateCount > 0 || updateCount == Statement.SUCCESS_NO_INFO) {
                    insertedOutboxes.add(outboxes.get(outboxIndex));
                }
                outboxIndex++;
            }
        }
        return insertedOutboxes;
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
