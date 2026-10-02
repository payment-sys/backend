package com.v_payment.pay.product.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductJdbcRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void changeQuantityBatch(Map<Long, Integer> successChangeQuantities) {
        if (successChangeQuantities == null || successChangeQuantities.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                """
                update product
                set stock_quantity = stock_quantity + ?
                where product_id = ?
                """,
                successChangeQuantities.entrySet(),
                successChangeQuantities.size(),
                (ps, entry) -> {
                    ps.setInt(1, entry.getValue());
                    ps.setLong(2, entry.getKey());
                }
        );
    }
}
