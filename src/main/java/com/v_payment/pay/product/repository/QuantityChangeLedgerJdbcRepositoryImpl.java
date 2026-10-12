package com.v_payment.pay.product.repository;

import com.v_payment.pay.product.domain.entity.QuantityChangeLedger;
import com.v_payment.pay.product.entrypoint.dto.msg.QuantityChangeMessage;
import com.v_payment.pay.product.entrypoint.dto.msg.QuantityChangeType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@Repository
public class QuantityChangeLedgerJdbcRepositoryImpl implements QuantityChangeLedgerJdbcRepository {
    private static final String FIND_EXISTING_LEDGER = """
                select
                    quantity_change_ledger_id,
                    order_code,
                    product_id,
                    change_type,
                    change_count,
                    stock_before,
                    stock_after,
                    created_at
                from quantity_change_ledger
                where %s
                """;

    private static final String LEDGER_BATCH = """
                insert ignore into quantity_change_ledger (
                    order_code,
                    product_id,
                    change_type,
                    change_count,
                    stock_before,
                    stock_after,
                    created_at
                ) values (?, ?, ?, ?, ?, ?, ?)
                """;

    private final JdbcTemplate jdbcTemplate;

    public QuantityChangeLedgerJdbcRepositoryImpl(
            @Qualifier("productJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void createLedgerBatch(List<QuantityChangeLedger> ledgers) {
        if (ledgers == null || ledgers.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(LEDGER_BATCH, ledgers, 100, (ps, ledger) -> {
            ps.setString(1, ledger.getOrderCode());
            ps.setLong(2, ledger.getProductId());
            ps.setString(3, ledger.getChangeType().name());
            ps.setInt(4, ledger.getChangeCount());
            ps.setObject(5, ledger.getStockBefore());
            ps.setObject(6, ledger.getStockAfter());
            ps.setObject(7, ledger.getCreatedAt());
        });
    }

    @Override
    public List<QuantityChangeLedger> findExistingLedgers(List<QuantityChangeMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        StringJoiner where = new StringJoiner(" or ");
        List<Object> params = new ArrayList<>();
        for (QuantityChangeMessage message : messages) {
            where.add("(order_code = ? and product_id = ? and change_type = ?)");
            params.add(message.orderCode());
            params.add(message.productId());
            params.add(message.type().name());
        }

        return jdbcTemplate.query(
                FIND_EXISTING_LEDGER.formatted(where.toString()),
                this::mapLedger,
                params.toArray()
        );
    }

    private QuantityChangeLedger mapLedger(ResultSet rs, int rowNum) throws SQLException {
        QuantityChangeMessage message = new QuantityChangeMessage(
                rs.getString("order_code"),
                rs.getLong("product_id"),
                rs.getInt("change_count"),
                1,
                QuantityChangeType.valueOf(rs.getString("change_type"))
        );
        Integer stockBefore = getNullableInteger(rs, "stock_before");
        Integer stockAfter = getNullableInteger(rs, "stock_after");
        LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);
        if (stockBefore == null || stockAfter == null) {
            return QuantityChangeLedger.success(message, createdAt);
        }
        return QuantityChangeLedger.success(message, stockBefore, stockAfter, createdAt);
    }

    private Integer getNullableInteger(ResultSet rs, String columnLabel) throws SQLException {
        int value = rs.getInt(columnLabel);
        if (rs.wasNull()) {
            return null;
        }
        return value;
    }
}
