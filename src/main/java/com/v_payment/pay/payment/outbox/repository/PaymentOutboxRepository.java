package com.v_payment.pay.payment.outbox.repository;

import com.v_payment.pay.payment.outbox.entity.PaymentOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long> {

    //todo: FOR UPDATE SKIP LOCKED 필요
    @NativeQuery("""
    SELECT po.payment_outbox_id AS paymentOutboxId,
           po.order_id AS orderId,
           po.payment_key AS paymentKey,
           po.amount AS amount
    FROM payment_outbox po
    WHERE po.next_attempt_time <= :now
    AND po.status = :status
    ORDER BY po.next_attempt_time ASC, po.payment_outbox_id ASC
    LIMIT :count
    FOR UPDATE SKIP LOCKED
    """)
    List<PaymentOutboxPublishProjection> findForPublish(@Param("status") String status,
                                                        @Param("now") LocalDateTime now,
                                                        @Param("count") int count);

    @Modifying
    @NativeQuery("""
    UPDATE payment_outbox FORCE INDEX (PRIMARY)
    SET status = 'PROCESSING', attempt_count = attempt_count + 1 
    WHERE payment_outbox_id IN (:ids)
    AND status = 'READY'
    """)
    int markProcessing(@Param("ids") List<Long> ids);

    @Modifying
    @NativeQuery("""
    UPDATE payment_outbox FORCE INDEX (PRIMARY)
    SET status = 'PUBLISHED'
    WHERE payment_outbox_id = :id
    AND status = 'PROCESSING'
    """)
    int markPublished(@Param("id") Long id);

    @Modifying
    @NativeQuery("""
    UPDATE payment_outbox FORCE INDEX (PRIMARY)
    SET status = 'READY',
        last_error_code = :lastErrorCode,
        last_error_message = :lastErrorMessage,
        next_attempt_time = :nextAttemptTime
    WHERE payment_outbox_id = :id
    AND status = 'PROCESSING'
    AND attempt_count < :maxAttemptCount
    """)
    int markReadyForRetry(@Param("id") Long id,
                          @Param("lastErrorCode") String lastErrorCode,
                          @Param("lastErrorMessage") String lastErrorMessage,
                          @Param("nextAttemptTime") LocalDateTime nextAttemptTime,
                          @Param("maxAttemptCount") int maxAttemptCount);

    @Modifying
    @NativeQuery("""
    UPDATE payment_outbox FORCE INDEX (PRIMARY)
    SET status = 'DEAD',
        last_error_code = :lastErrorCode,
        last_error_message = :lastErrorMessage,
        next_attempt_time = null
    WHERE payment_outbox_id = :id
    AND status = 'PROCESSING'
    """)
    int markDead(@Param("id") Long id,
                 @Param("lastErrorCode") String lastErrorCode,
                 @Param("lastErrorMessage") String lastErrorMessage);

}
