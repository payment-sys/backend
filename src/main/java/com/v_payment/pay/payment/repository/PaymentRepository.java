package com.v_payment.pay.payment.repository;

import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("""
    SELECT p
    FROM Payment p
    WHERE p.orderId = :orderId
    AND p.paymentStatus = :paymentStatus
    """)
    Optional<Payment> findByOrderId(String orderId, PaymentStatus paymentStatus);
}
