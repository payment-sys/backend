package com.v_payment.pay.product.repository;

import com.v_payment.pay.product.entity.ProductQuantityEvent;
import com.v_payment.pay.product.entity.ProductQuantityEventStatus;
import org.springframework.data.domain.Pageable;
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
        SELECT *
        FROM product_quantity_event FORCE INDEX (idx_pqe_status_id)
        WHERE status = :status
        ORDER BY product_quantity_event_id ASC
        LIMIT :limit
        """,
            nativeQuery = true
    )
    List<ProductQuantityEvent> findByProductQuantityEventStatusOrderByIdAsc(
            @Param("status") String status,
            @Param("limit") int limit
    );

    @Query("""
            select e
            from ProductQuantityEvent e
            where e.productQuantityEventStatus = com.v_payment.pay.product.entity.ProductQuantityEventStatus.RETRY
              and e.nextAttemptTime <= :now
            order by e.nextAttemptTime asc, e.id asc
            """)
    List<ProductQuantityEvent> findRetryable(@Param("now") LocalDateTime now, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ProductQuantityEvent e
            set e.productQuantityEventStatus = :status,
                e.nextAttemptTime = null,
                e.updatedAt = :updatedAt
            where e.id in :ids
            """)
    int updateStatusByIds(@Param("ids") List<Long> ids,
                          @Param("status") ProductQuantityEventStatus status,
                          @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ProductQuantityEvent e
            set e.productQuantityEventStatus = com.v_payment.pay.product.entity.ProductQuantityEventStatus.RETRY,
                e.retryCount = coalesce(e.retryCount, 0) + 1,
                e.nextAttemptTime = :nextAttemptTime,
                e.updatedAt = :updatedAt
            where e.id in :ids
            """)
    int markRetryByIds(@Param("ids") List<Long> ids,
                       @Param("nextAttemptTime") LocalDateTime nextAttemptTime,
                       @Param("updatedAt") LocalDateTime updatedAt);
}
