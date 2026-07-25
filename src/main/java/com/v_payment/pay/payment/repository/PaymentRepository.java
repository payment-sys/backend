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

    Optional<Payment> findByOrderCodeAndPaymentStatus(String orderCode, PaymentStatus paymentStatus);

    @Modifying
    @Query("""
    UPDATE Payment p
    SET p.paymentStatus = :inProgressStatus,
        p.paymentKey = :paymentKey
    WHERE p.orderCode = :orderCode
    AND p.paymentStatus = :readyStatus
    AND p.requestedAmount = :requestedAmount
    AND p.provider = :provider
    AND p.paymentMethod = :paymentMethod
    """)
    int markInProgress(@Param("orderCode") String orderCode,
                       @Param("paymentKey") String paymentKey,
                       @Param("requestedAmount") Long requestedAmount,
                       @Param("provider") Provider provider,
                       @Param("paymentMethod") PaymentMethod paymentMethod,
                       @Param("readyStatus") PaymentStatus readyStatus,
                       @Param("inProgressStatus") PaymentStatus inProgressStatus);

    @Modifying
    @Query("""
    UPDATE Payment p
    SET p.paymentStatus = :doneStatus,
        p.approvedAmount = :approvedAmount,
        p.approvedAt = :approvedAt,
        p.receiptUrl = :receiptUrl
    WHERE p.orderCode = :orderCode
    AND p.paymentStatus = :inProgressStatus
    """)
    int markDone(@Param("orderCode") String orderCode,
                 @Param("inProgressStatus") PaymentStatus inProgressStatus,
                 @Param("doneStatus") PaymentStatus doneStatus,
                 @Param("approvedAmount") Long approvedAmount,
                 @Param("approvedAt") LocalDateTime approvedAt,
                 @Param("receiptUrl") String receiptUrl);

    @Modifying
    @Query("""
    UPDATE Payment p
    SET p.paymentStatus = :abortedStatus
    WHERE p.orderCode = :orderCode
    AND p.paymentStatus = :inProgressStatus
    """)
    int markAborted(@Param("orderCode") String orderCode,
                    @Param("inProgressStatus") PaymentStatus inProgressStatus,
                    @Param("abortedStatus") PaymentStatus abortedStatus);

    @Modifying
    @Query("""
    UPDATE Payment p
    SET p.paymentStatus = :unknownStatus
    WHERE p.orderCode = :orderCode
    AND p.paymentStatus = :inProgressStatus
    """)
    int markUnknown(@Param("orderCode") String orderCode,
                    @Param("inProgressStatus") PaymentStatus inProgressStatus,
                    @Param("unknownStatus") PaymentStatus unknownStatus);

    @Modifying
    @Query("""
    UPDATE Payment p
    SET p.paymentStatus = :doneStatus,
        p.paymentKey = :paymentKey,
        p.approvedAmount = :approvedAmount,
        p.approvedAt = :approvedAt,
        p.receiptUrl = :receiptUrl
    WHERE p.orderCode = :orderCode
    AND p.paymentStatus <> :doneStatus
    """)
    int markDone(@Param("orderCode") String orderCode,
                 @Param("paymentKey") String paymentKey,
                 @Param("doneStatus") PaymentStatus doneStatus,
                 @Param("approvedAmount") Long approvedAmount,
                 @Param("approvedAt") LocalDateTime approvedAt,
                 @Param("receiptUrl") String receiptUrl);

    @Modifying
    @Query("""
    UPDATE Payment p
    SET p.paymentStatus = :abortedStatus,
        p.paymentKey = :paymentKey
    WHERE p.orderCode = :orderCode
    AND p.paymentStatus <> :doneStatus
    """)
    int markAborted(@Param("orderCode") String orderCode,
                    @Param("paymentKey") String paymentKey,
                    @Param("abortedStatus") PaymentStatus abortedStatus,
                    @Param("doneStatus") PaymentStatus doneStatus);

    @Modifying
    @Query("""
    UPDATE Payment p
    SET p.paymentStatus = :expiredStatus,
        p.paymentKey = :paymentKey
    WHERE p.orderCode = :orderCode
    AND p.paymentStatus <> :doneStatus
    """)
    int markExpired(@Param("orderCode") String orderCode,
                    @Param("paymentKey") String paymentKey,
                    @Param("expiredStatus") PaymentStatus expiredStatus,
                    @Param("doneStatus") PaymentStatus doneStatus);
}
