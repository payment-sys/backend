package com.v_payment.pay.product.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class ProductRepositoryImpl implements ProductJdbcRepository {
    private final JdbcTemplate jdbcTemplate;

    public ProductRepositoryImpl(@Qualifier("productJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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
