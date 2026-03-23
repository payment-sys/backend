package com.v_payment.pay.event.repository;

import com.v_payment.pay.event.entity.PaymentEvent;
import com.v_payment.pay.event.entity.PaymentEventStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {

    @Query(value = """
    SELECT *
    FROM payment_event pe
    WHERE pe.payment_event_status = 'PENDING'
    ORDER BY pe.payment_event_id
    LIMIT :count
    FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<PaymentEvent> findAllByPendingStatusLimit(int count);

    @Query("""
    SELECT pe
    FROM PaymentEvent pe
    WHERE pe.orderId = :orderId
    """)
    PaymentEvent findByOrderId(String orderId);
}
