package com.v_payment.pay.payment.repository;

import com.v_payment.pay.payment.domain.entity.Payment;
import com.v_payment.pay.payment.domain.entity.PaymentMethod;
import com.v_payment.pay.payment.domain.entity.PaymentStatus;
import com.v_payment.pay.payment.domain.entity.Provider;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long>, PaymentBatchRepository {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findAllByOrderCode(String orderCode);

    @Query("""
    SELECT p
    FROM Payment p
    WHERE p.paymentStatus IN :paymentStatuses
    AND p.paymentKey IS NOT NULL
    AND p.requestedAt <= :requestedBefore
    ORDER BY p.requestedAt ASC
    """)
    List<Payment> findRecoverablePayments(@Param("paymentStatuses") List<PaymentStatus> paymentStatuses,
                                          @Param("requestedBefore") LocalDateTime requestedBefore,
                                          Pageable pageable);

    @Modifying
    @Query("""
    UPDATE Payment p
    SET p.paymentStatus = :inProgressStatus,
        p.paymentKey = :paymentKey
    WHERE p.idempotencyKey = :idempotencyKey
    AND p.paymentStatus = :readyStatus
    AND p.requestedAmount = :requestedAmount
    AND p.provider = :provider
    AND p.paymentMethod = :paymentMethod
    """)
    int markInProgress(@Param("idempotencyKey") String idempotencyKey,
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
    WHERE p.idempotencyKey = :idempotencyKey
    AND p.paymentStatus IN (:inProgressStatus, :unknownStatus, :doneStatus)
    """)
    int markDone(@Param("idempotencyKey") String idempotencyKey,
                 @Param("inProgressStatus") PaymentStatus inProgressStatus,
                 @Param("unknownStatus") PaymentStatus unknownStatus,
                 @Param("doneStatus") PaymentStatus doneStatus,
                 @Param("approvedAmount") Long approvedAmount,
                 @Param("approvedAt") LocalDateTime approvedAt,
                 @Param("receiptUrl") String receiptUrl);

    @Modifying
    @Query("""
    UPDATE Payment p
    SET p.paymentStatus = :abortedStatus
    WHERE p.idempotencyKey = :idempotencyKey
    AND p.paymentStatus IN (:inProgressStatus, :unknownStatus, :abortedStatus)
    """)
    int markAborted(@Param("idempotencyKey") String idempotencyKey,
                    @Param("inProgressStatus") PaymentStatus inProgressStatus,
                    @Param("unknownStatus") PaymentStatus unknownStatus,
                    @Param("abortedStatus") PaymentStatus abortedStatus);

    @Modifying
    @Query("""
    UPDATE Payment p
    SET p.paymentStatus = :unknownStatus
    WHERE p.idempotencyKey = :idempotencyKey
    AND p.paymentStatus IN (:inProgressStatus, :unknownStatus)
    """)
    int markUnknown(@Param("idempotencyKey") String idempotencyKey,
                    @Param("inProgressStatus") PaymentStatus inProgressStatus,
                    @Param("unknownStatus") PaymentStatus unknownStatus);

    @Modifying
    @Query("""
    UPDATE Payment p
    SET p.paymentStatus = :expiredStatus
    WHERE p.idempotencyKey = :idempotencyKey
    AND p.paymentStatus IN (:inProgressStatus, :unknownStatus, :expiredStatus)
    """)
    int markExpired(@Param("idempotencyKey") String idempotencyKey,
                    @Param("inProgressStatus") PaymentStatus inProgressStatus,
                    @Param("unknownStatus") PaymentStatus unknownStatus,
                    @Param("expiredStatus") PaymentStatus expiredStatus);

    @Modifying
    @Query("""
    UPDATE Payment p
    SET p.recoveryAttemptCount = COALESCE(p.recoveryAttemptCount, 0) + 1
    WHERE p.idempotencyKey = :idempotencyKey
    AND p.paymentStatus IN :recoverableStatuses
    """)
    int increaseRecoveryAttemptCount(@Param("idempotencyKey") String idempotencyKey,
                                     @Param("recoverableStatuses") List<PaymentStatus> recoverableStatuses);

    @Modifying
    @Query("""
    UPDATE Payment p
    SET p.paymentStatus = :doneStatus,
        p.paymentKey = :paymentKey,
        p.approvedAmount = :approvedAmount,
        p.approvedAt = :approvedAt,
        p.receiptUrl = :receiptUrl
    WHERE p.idempotencyKey = :idempotencyKey
    AND p.paymentStatus <> :doneStatus
    """)
    int markDone(@Param("idempotencyKey") String idempotencyKey,
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
    WHERE p.idempotencyKey = :idempotencyKey
    AND p.paymentStatus <> :doneStatus
    """)
    int markAborted(@Param("idempotencyKey") String idempotencyKey,
                    @Param("paymentKey") String paymentKey,
                    @Param("abortedStatus") PaymentStatus abortedStatus,
                    @Param("doneStatus") PaymentStatus doneStatus);

    @Modifying
    @Query("""
    UPDATE Payment p
    SET p.paymentStatus = :expiredStatus,
        p.paymentKey = :paymentKey
    WHERE p.idempotencyKey = :idempotencyKey
    AND p.paymentStatus <> :doneStatus
    """)
    int markExpired(@Param("idempotencyKey") String idempotencyKey,
                    @Param("paymentKey") String paymentKey,
                    @Param("expiredStatus") PaymentStatus expiredStatus,
                    @Param("doneStatus") PaymentStatus doneStatus);
}
