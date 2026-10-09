package com.v_payment.pay.order.infrastructure.persistence.repository;

import com.v_payment.pay.order.domain.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Repository
public class OrderJdbcRepositoryImpl implements OrderJdbcRepository {
    private static final String UPDATE_SUMMARY_BATCH = """
            update orders
            set status = ?,
                quantity_change_success_product_ids = ?,
                quantity_change_failed_product_ids = ?
            where order_code = ?
              and status = ?
            """;

    private static final String UPDATE_SUMMARY_BATCH_H2 = """
            update orders
            set status = ?,
                quantity_change_success_product_ids = ? format json,
                quantity_change_failed_product_ids = ? format json
            where order_code = ?
              and status = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final String updateSummaryBatchSql;

    public OrderJdbcRepositoryImpl(
            @Qualifier("orderPaymentJdbcTemplate") JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.updateSummaryBatchSql = isH2(jdbcTemplate) ? UPDATE_SUMMARY_BATCH_H2 : UPDATE_SUMMARY_BATCH;
    }

    @Override
    public void updateSummaryBatch(List<OrderSummaryUpdate> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(updateSummaryBatchSql, updates, 100, (ps, update) -> {
            ps.setString(1, update.targetStatus().name());
            ps.setString(2, objectMapper.writeValueAsString(update.successProductIds()));
            ps.setString(3, objectMapper.writeValueAsString(update.failedProductIds()));
            ps.setString(4, update.orderCode());
            ps.setString(5, OrderStatus.CREATED.name());
        });
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
