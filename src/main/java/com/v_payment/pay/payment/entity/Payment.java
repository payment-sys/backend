package com.v_payment.pay.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * unit test
 * DisplayName: Payment 엔티티 생성 시 paymentKey, approvedAmount, approvedAt, receiptUrl을 제외한 나머지 필드의 값이 세팅된다.
 * Given : 엔티티의 모든 필드 값
 * When : createPendingPayment 호출
 * Then : 값과 생성된 payment의 값이 같아야 한다.
 */

@Getter
@Entity
@NoArgsConstructor
@Table(name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_order_code", columnNames = "order_code")
        })
public class Payment {
    @Id
    @Column(name = "payment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "order_code")
    private String orderCode;

    private String paymentKey;

    private Long requestedAmount;

    private Long approvedAmount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private LocalDateTime requestedAt;

    private LocalDateTime approvedAt;

    private String receiptUrl;

    private Integer recoveryAttemptCount;

    @Builder
    public Payment(Provider provider,
                   PaymentMethod paymentMethod,
                   String orderCode,
                   String paymentKey,
                   Long requestedAmount,
                   Long approvedAmount,
                   PaymentStatus paymentStatus,
                   LocalDateTime requestedAt,
                   LocalDateTime approvedAt,
                   String receiptUrl,
                   Integer recoveryAttemptCount) {
        this.provider = provider;
        this.paymentMethod = paymentMethod;
        this.orderCode = orderCode;
        this.paymentKey = paymentKey;
        this.requestedAmount = requestedAmount;
        this.approvedAmount = approvedAmount;
        this.paymentStatus = paymentStatus;
        this.requestedAt = requestedAt;
        this.approvedAt = approvedAt;
        this.receiptUrl = receiptUrl;
        this.recoveryAttemptCount = recoveryAttemptCount;
    }

    public static Payment createPendingPayment(String orderCode, Long amount, PaymentMethod paymentMethod, Clock clock) {
        return Payment.builder()
                .provider(Provider.TOSS)
                .paymentMethod(paymentMethod)
                .orderCode(orderCode)
                .paymentKey(null)
                .requestedAmount(amount)
                .approvedAmount(null)
                .paymentStatus(PaymentStatus.READY)
                .requestedAt(LocalDateTime.now(clock))
                .approvedAt(null)
                .receiptUrl(null)
                .recoveryAttemptCount(0)
                .build();
    }
}
