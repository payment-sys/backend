package com.v_payment.pay.order.infrastructure.persistence.repository;

import com.v_payment.pay.order.domain.outbox.QuantityChangeOutbox;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class QuantityChangeOutboxJdbcRepositoryImpl implements QuantityChangeOutboxJdbcRepository {
    private static final String OUTBOX_BATCH = """
            insert ignore into quantity_change_outbox (
                order_code,
                requested_order,
                outbox_type,
                status,
                retry_count,
                next_attempt_time,
                created_at,
                updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String OUTBOX_BATCH_H2 = """
            insert ignore into quantity_change_outbox (
                order_code,
                requested_order,
                outbox_type,
                status,
                retry_count,
                next_attempt_time,
                created_at,
                updated_at
            ) values (?, ? format json, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final String outboxBatchSql;

    public QuantityChangeOutboxJdbcRepositoryImpl(
            @Qualifier("orderPaymentJdbcTemplate") JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.outboxBatchSql = isH2(jdbcTemplate) ? OUTBOX_BATCH_H2 : OUTBOX_BATCH;
    }

    @Override
    public List<QuantityChangeOutbox> createOutboxBatch(List<QuantityChangeOutbox> outboxes) {
        if (outboxes == null || outboxes.isEmpty()) {
            return List.of();
        }

        int[][] updateCounts = jdbcTemplate.batchUpdate(outboxBatchSql, outboxes, 100, (ps, outbox) -> {
            ps.setString(1, outbox.getOrderCode());
            ps.setString(2, objectMapper.writeValueAsString(outbox.getRequestedOrder()));
            ps.setString(3, outbox.getType().name());
            ps.setString(4, outbox.getStatus().name());
            ps.setInt(5, outbox.getRetryCount());
            ps.setObject(6, outbox.getNextAttemptTime());
            ps.setObject(7, outbox.getCreatedAt());
            ps.setObject(8, outbox.getUpdatedAt());
        });

        List<QuantityChangeOutbox> insertedOutboxes = new ArrayList<>();
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

    private boolean isH2(JdbcTemplate jdbcTemplate) {
        DataSource dataSource = jdbcTemplate.getDataSource();
        if (dataSource == null) {
            return false;
        }

        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().contains("H2");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to resolve database product name.", e);
        }
    }
}
