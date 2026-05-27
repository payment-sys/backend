package com.v_payment.pay.payment.repository;

import com.v_payment.pay.payment.entity.PaymentOutbox;
import com.v_payment.pay.payment.entity.PaymentOutboxStatus;
import com.v_payment.pay.payment.entity.PaymentPayload;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long> {

    @Query("""
    SELECT po.id
    FROM PaymentOutbox po
    WHERE po.nextAttemptTime <= :now
    AND po.status = :status
    ORDER BY po.nextAttemptTime ASC, po.id ASC
    """)
    List<Long> findForPublish(@Param("status") PaymentOutboxStatus status,
                              @Param("now") LocalDateTime now,
                              Pageable pageable);

    @Query("""
    SELECT new com.v_payment.pay.payment.entity.PaymentPayload(po.orderId, po.paymentKey, po.amount)
    FROM PaymentOutbox po
    WHERE po.id = :id
    """)
    Optional<PaymentPayload> findPaymentPayloadById(@Param("id") Long id);

    Optional<PaymentOutbox> findByIdAndStatus(Long id, PaymentOutboxStatus status);
}
