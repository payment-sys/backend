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
    public void decreaseProducts(Map<Long, Integer> decreaseTotal) {
        if(decreaseTotal.isEmpty()) return;

        jdbcTemplate.batchUpdate(
                """
                update product
                set stock_quantity = stock_quantity + ?
                where product_id = ?
                """,
                decreaseTotal.entrySet(),
                decreaseTotal.size(),
                (ps, entry) -> {
                    ps.setInt(1, entry.getValue());
                    ps.setLong(2, entry.getKey());
                }
        );
    }
}
