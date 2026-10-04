package com.v_payment.pay.product.repository;

import com.v_payment.pay.product.domain.entity.QuantityChangeResultOutbox;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface QuantityChangeResultOutboxRepository extends JpaRepository<QuantityChangeResultOutbox, Long>, QuantityChangeResultOutboxJdbcRepository {
    @Query(
            value = """
                    SELECT *
                    FROM quantity_change_result_outbox
                    WHERE status = :status
                      AND (next_attempt_time IS NULL OR next_attempt_time <= :now)
                      AND created_at <= :publishBefore
                    ORDER BY quantity_change_result_outbox_id ASC
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<QuantityChangeResultOutbox> findReadyForPublish(
            @Param("status") String status,
            @Param("now") LocalDateTime now,
            @Param("publishBefore") LocalDateTime publishBefore,
            @Param("limit") int limit
    );
}
