package com.v_payment.pay.product.repository;

import com.v_payment.pay.product.domain.entity.ProductQuantityEvent;
import com.v_payment.pay.product.domain.entity.ProductQuantityEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductQuantityEventRepository extends JpaRepository<ProductQuantityEvent, Long> {
    @Query(
            value = """
        SELECT /*+ JOIN_INDEX(e idx_pqe_status_id) ORDER_INDEX(e idx_pqe_status_id) */ e.*
        FROM product_quantity_event e
        WHERE status = :status
        ORDER BY product_quantity_event_id ASC
        LIMIT :limit 
        FOR UPDATE SKIP LOCKED
        """,
            nativeQuery = true
    )
    List<ProductQuantityEvent> findReadyProductQuantityEvents(
            @Param("status") String status,
            @Param("limit") int limit
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ProductQuantityEvent e
            set e.productQuantityEventStatus = :status,
                e.updatedAt = :updatedAt
            where e.id in :ids
            and e.productQuantityEventStatus = com.v_payment.pay.product.domain.entity.ProductQuantityEventStatus.READY
            """)
    int updateStatusByIds(@Param("ids") List<Long> ids,
                          @Param("status") ProductQuantityEventStatus status,
                          @Param("updatedAt") LocalDateTime updatedAt);
}
