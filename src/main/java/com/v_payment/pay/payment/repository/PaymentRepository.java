package com.v_payment.pay.payment.repository;

import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentMethod;
import com.v_payment.pay.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.entity.Provider;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderIdAndPaymentStatus(String orderId, PaymentStatus paymentStatus);

    @Modifying
    @Query("""
    UPDATE Payment p
    SET p.paymentStatus = :approvingStatus,
        p.paymentKey = :paymentKey,
        p.version = p.version + 1
    WHERE p.orderId = :orderId
    AND p.paymentStatus = :pendingStatus
    AND p.requestedAmount = :requestedAmount
    AND p.provider = :provider
    AND p.paymentMethod = :paymentMethod
    """)
    int markApproving(@Param("orderId") String orderId,
                      @Param("paymentKey") String paymentKey,
                      @Param("requestedAmount") Long requestedAmount,
                      @Param("provider") Provider provider,
                      @Param("paymentMethod") PaymentMethod paymentMethod,
                      @Param("pendingStatus") PaymentStatus pendingStatus,
                      @Param("approvingStatus") PaymentStatus approvingStatus);

    @Modifying
    @Query("""
    UPDATE Payment p
    SET p.paymentStatus = :approvedStatus,
        p.approvedAmount = :approvedAmount,
        p.approvedAt = :approvedAt,
        p.receiptUrl = :receiptUrl,
        p.version = p.version + 1
    WHERE p.orderId = :orderId
    AND p.paymentStatus = :approvingStatus
    """)
    int markApproved(@Param("orderId") String orderId,
                     @Param("approvingStatus") PaymentStatus approvingStatus,
                     @Param("approvedStatus") PaymentStatus approvedStatus,
                     @Param("approvedAmount") Long approvedAmount,
                     @Param("approvedAt") LocalDateTime approvedAt,
                     @Param("receiptUrl") String receiptUrl);

    @Modifying
    @Query("""
    UPDATE Payment p
    SET p.paymentStatus = :rejectedStatus,
        p.failedMessage = :failedMessage,
        p.version = p.version + 1
    WHERE p.orderId = :orderId
    AND p.paymentStatus = :approvingStatus
    """)
    int markRejected(@Param("orderId") String orderId,
                     @Param("approvingStatus") PaymentStatus approvingStatus,
                     @Param("rejectedStatus") PaymentStatus rejectedStatus,
                     @Param("failedMessage") String failedMessage);

}
