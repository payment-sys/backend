package com.v_payment.pay.order.repository;

import com.v_payment.pay.order.domain.orderitem.OrderItemStatus;
import com.v_payment.pay.order.infra.kafka.dto.QuantityChangeResultMessage;
import com.v_payment.pay.product.domain.entity.ChangeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderItemJdbcRepositoryImpl implements OrderItemJdbcRepository {
    private static final String UPDATE_STATUS_BY_RESULT = """
            update order_item oi
            join orders o on oi.order_id = o.order_id
            set oi.status = ?
            where o.order_code = ?
              and oi.product_id = ?
              and oi.status = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void updateStatusByQuantityChangeResults(
            List<QuantityChangeResultMessage> results,
            OrderItemStatus processingStatus,
            OrderItemStatus successStatus,
            OrderItemStatus failStatus
    ) {
        if (results == null || results.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(UPDATE_STATUS_BY_RESULT, results, results.size(), (ps, result) -> {
            OrderItemStatus targetStatus = result.changeStatus() == ChangeStatus.SUCCESS ? successStatus : failStatus;
            ps.setString(1, targetStatus.name());
            ps.setString(2, result.orderCode());
            ps.setLong(3, result.productId());
            ps.setString(4, processingStatus.name());
        });
    }
}
