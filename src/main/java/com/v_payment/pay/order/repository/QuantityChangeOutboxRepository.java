package com.v_payment.pay.order.repository;

import com.v_payment.pay.order.domain.outbox.QuantityChangeOutbox;
import com.v_payment.pay.order.domain.outbox.QuantityChangeOutboxStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface QuantityChangeOutboxRepository extends JpaRepository<QuantityChangeOutbox, Long> {
    @Query(
            value = """
                    SELECT *
                    FROM quantity_change_outbox
                    WHERE status = :status
                    ORDER BY quantity_change_outbox_id ASC
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<QuantityChangeOutbox> findReadyForPublish(
            @Param("status") String status,
            @Param("limit") int limit
    );

    @Modifying
    @Query("""
            update QuantityChangeOutbox o
            set o.status = :doneStatus,
                o.updatedAt = :updatedAt
            where o.id = :id
              and o.status = :readyStatus
            """)
    int markDone(
            @Param("id") Long id,
            @Param("readyStatus") QuantityChangeOutboxStatus readyStatus,
            @Param("doneStatus") QuantityChangeOutboxStatus doneStatus,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
