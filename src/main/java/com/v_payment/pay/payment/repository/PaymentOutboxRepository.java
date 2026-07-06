package com.v_payment.pay.payment.repository;

import com.v_payment.pay.payment.entity.PaymentOutbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long> {

    @NativeQuery("""
    SELECT po.payment_outbox_id AS paymentOutboxId,
           po.order_id AS orderId,
           po.payment_key AS paymentKey,
           po.amount AS amount
    FROM payment_outbox po
    WHERE po.status = 'READY'
    AND po.created_at <= :cutoff
    ORDER BY po.created_at ASC, po.payment_outbox_id ASC
    LIMIT :limit
    """)
    List<PaymentOutboxTaskProjection> findStaleReady(@Param("cutoff") LocalDateTime cutoff,
                                                     @Param("limit") int limit);

    @Modifying
    @NativeQuery("""
    UPDATE payment_outbox FORCE INDEX (PRIMARY)
    SET status = 'PROCESSING',
        processing_started_at = :processingStartedAt
    WHERE payment_outbox_id = :id
    AND status = 'READY'
    """)
    int markProcessing(@Param("id") Long id,
                       @Param("processingStartedAt") LocalDateTime processingStartedAt);

    @Modifying
    @NativeQuery("""
    UPDATE payment_outbox FORCE INDEX (PRIMARY)
    SET status = 'PUBLISHED',
        published_at = :publishedAt
    WHERE payment_outbox_id = :id
    AND status = 'PROCESSING'
    """)
    int markPublished(@Param("id") Long id,
                      @Param("publishedAt") LocalDateTime publishedAt);
}
