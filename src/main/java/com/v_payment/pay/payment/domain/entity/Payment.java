package com.v_payment.pay.payment.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_idempotency_key", columnNames = "idempotency_key")
        })
public class Payment {
    @Id
    @Column(name = "payment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "order_code", nullable = false)
    private String orderCode;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    private String paymentKey;

    @Column(nullable = false)
    private Long requestedAmount;

    private Long approvedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime approvedAt;

    private String receiptUrl;

    @Column(nullable = false)
    private Integer recoveryAttemptCount;

    @Builder
    private Payment(Provider provider,
                    PaymentMethod paymentMethod,
                    String orderCode,
                    String idempotencyKey,
                    String paymentKey,
                    Long requestedAmount,
                    Long approvedAmount,
                    PaymentStatus paymentStatus,
                    LocalDateTime requestedAt,
                    LocalDateTime approvedAt,
                    String receiptUrl,
                    Integer recoveryAttemptCount) {
        this.provider = validateProvider(provider);
        this.paymentMethod = validatePaymentMethod(paymentMethod);
        this.orderCode = validateOrderCode(orderCode);
        this.idempotencyKey = validateIdempotencyKey(idempotencyKey);
        this.paymentKey = paymentKey;
        this.requestedAmount = validateRequestedAmount(requestedAmount);
        this.approvedAmount = approvedAmount;
        this.paymentStatus = validatePaymentStatus(paymentStatus);
        this.requestedAt = validateRequestedAt(requestedAt);
        this.approvedAt = approvedAt;
        this.receiptUrl = receiptUrl;
        this.recoveryAttemptCount = validateRecoveryAttemptCount(recoveryAttemptCount);
    }

    public static Payment createReady(
            String orderCode,
            String idempotencyKey,
            Long amount,
            PaymentMethod paymentMethod,
            Clock clock
    ) {
        return Payment.builder()
                .provider(Provider.TOSS)
                .paymentMethod(paymentMethod)
                .orderCode(orderCode)
                .idempotencyKey(idempotencyKey)
                .paymentKey(null)
                .requestedAmount(amount)
                .approvedAmount(null)
                .paymentStatus(PaymentStatus.READY)
                .requestedAt(LocalDateTime.now(validateClock(clock)))
                .approvedAt(null)
                .receiptUrl(null)
                .recoveryAttemptCount(0)
                .build();
    }

    public static Payment createPendingPayment(String orderCode, Long amount, PaymentMethod paymentMethod, Clock clock) {
        return createReady(orderCode, orderCode, amount, paymentMethod, clock);
    }

    public boolean isReady() {
        return paymentStatus == PaymentStatus.READY;
    }

    public boolean isSameRequestedAmount(Long requestedAmount) {
        return this.requestedAmount.equals(requestedAmount);
    }

    public boolean isSameProvider(Provider provider) {
        return this.provider == provider;
    }

    public boolean isSamePaymentMethod(PaymentMethod paymentMethod) {
        return this.paymentMethod == paymentMethod;
    }

    public void markInProgress(String paymentKey) {
        this.paymentKey = validatePaymentKey(paymentKey);
        this.paymentStatus = PaymentStatus.IN_PROGRESS;
    }

    public void markDone(Long approvedAmount, LocalDateTime approvedAt, String receiptUrl) {
        this.approvedAmount = validateApprovedAmount(approvedAmount);
        this.approvedAt = validateApprovedAt(approvedAt);
        this.receiptUrl = receiptUrl;
        this.paymentStatus = PaymentStatus.DONE;
    }

    public void markAborted() {
        this.paymentStatus = PaymentStatus.ABORTED;
    }

    public void markUnknown() {
        this.paymentStatus = PaymentStatus.UNKNOWN;
    }

    public void markExpired() {
        this.paymentStatus = PaymentStatus.EXPIRED;
    }

    public void increaseRecoveryAttemptCount() {
        this.recoveryAttemptCount++;
    }

    private Provider validateProvider(Provider provider) {
        if (provider == null) throw new IllegalArgumentException("provider는 필수입니다.");
        return provider;
    }

    private PaymentMethod validatePaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) throw new IllegalArgumentException("paymentMethod는 필수입니다.");
        return paymentMethod;
    }

    private String validateOrderCode(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) throw new IllegalArgumentException("orderCode는 필수입니다.");
        return orderCode;
    }

    private String validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey는 필수입니다.");
        }
        return idempotencyKey;
    }

    private Long validateRequestedAmount(Long requestedAmount) {
        if (requestedAmount == null) throw new IllegalArgumentException("requestedAmount는 필수입니다.");
        if (requestedAmount < 0) throw new IllegalArgumentException("requestedAmount는 음수일 수 없습니다.");
        return requestedAmount;
    }

    private PaymentStatus validatePaymentStatus(PaymentStatus paymentStatus) {
        if (paymentStatus == null) throw new IllegalArgumentException("paymentStatus는 필수입니다.");
        return paymentStatus;
    }

    private LocalDateTime validateRequestedAt(LocalDateTime requestedAt) {
        if (requestedAt == null) throw new IllegalArgumentException("requestedAt은 필수입니다.");
        return requestedAt;
    }

    private Integer validateRecoveryAttemptCount(Integer recoveryAttemptCount) {
        if (recoveryAttemptCount == null) throw new IllegalArgumentException("recoveryAttemptCount는 필수입니다.");
        if (recoveryAttemptCount < 0) throw new IllegalArgumentException("recoveryAttemptCount는 음수일 수 없습니다.");
        return recoveryAttemptCount;
    }

    private String validatePaymentKey(String paymentKey) {
        if (paymentKey == null || paymentKey.isBlank()) throw new IllegalArgumentException("paymentKey는 필수입니다.");
        return paymentKey;
    }

    private Long validateApprovedAmount(Long approvedAmount) {
        if (approvedAmount == null) throw new IllegalArgumentException("approvedAmount는 필수입니다.");
        if (approvedAmount < 0) throw new IllegalArgumentException("approvedAmount는 음수일 수 없습니다.");
        return approvedAmount;
    }

    private LocalDateTime validateApprovedAt(LocalDateTime approvedAt) {
        if (approvedAt == null) throw new IllegalArgumentException("approvedAt은 필수입니다.");
        return approvedAt;
    }

    private static Clock validateClock(Clock clock) {
        if (clock == null) throw new IllegalArgumentException("clock은 필수입니다.");
        return clock;
    }
}
