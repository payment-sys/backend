package com.v_payment.pay.payment.entity;

import com.v_payment.pay.payment.controller.dto.req.PaymentCreateReq;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "payment")
public class Payment {
    @Id
    @Column(name = "payment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String orderId;

    private String paymentKey;

    private Long requestedAmount;

    private Long approvedAmount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private LocalDateTime requestedAt;

    private LocalDateTime approvedAt;

    private String receiptUrl;

    @Builder
    public Payment(Provider provider,
                   PaymentMethod paymentMethod,
                   String orderId,
                   String paymentKey,
                   Long requestedAmount,
                   Long approvedAmount,
                   PaymentStatus paymentStatus,
                   LocalDateTime requestedAt,
                   LocalDateTime approvedAt,
                   String receiptUrl) {
        this.provider = provider;
        this.paymentMethod = paymentMethod;
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.requestedAmount = requestedAmount;
        this.approvedAmount = approvedAmount;
        this.paymentStatus = paymentStatus;
        this.requestedAt = requestedAt;
        this.approvedAt = approvedAt;
        this.receiptUrl = receiptUrl;
    }

    public PaymentPayload getPaymentPayload() {
        return PaymentPayload.create(orderId, paymentKey, requestedAmount);
    }

    public static Payment create(PaymentCreateReq paymentCreateReq, Clock clock) {
        return Payment.builder()
                .provider(Provider.TOSS)
                .paymentMethod(paymentCreateReq.paymentMethod())
                .orderId(UUID.randomUUID().toString())
                .paymentKey(null)
                .requestedAmount(paymentCreateReq.requestedAmount())
                .approvedAmount(null)
                .paymentStatus(PaymentStatus.PENDING)
                .requestedAt(LocalDateTime.now(clock))
                .approvedAt(null)
                .receiptUrl(null)
                .build();
    }
}
