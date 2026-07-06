package com.v_payment.pay.payment.entity;

import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "payment_outbox",
        indexes = @Index(
                name = "idx_payment_outbox_publish",
                columnList = "status, created_at, payment_outbox_id"
        )
)
public class PaymentOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_outbox_id")
    private Long id;

    private String orderId;

    private String paymentKey;

    private Long amount;

    @Enumerated(EnumType.STRING)
    private PaymentOutboxStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime processingStartedAt;

    private LocalDateTime publishedAt;

    protected PaymentOutbox() {
    }

    @Builder
    private PaymentOutbox(
            String orderId,
            String paymentKey,
            Long amount,
            PaymentOutboxStatus status,
            LocalDateTime createdAt,
            LocalDateTime processingStartedAt,
            LocalDateTime publishedAt
    ) {
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.processingStartedAt = processingStartedAt;
        this.publishedAt = publishedAt;
    }

    public PaymentPayload getPaymentPayload() {
        return PaymentPayload.create(orderId, paymentKey, amount);
    }

    public static PaymentOutbox create(ApprovalReq approvalReq, LocalDateTime createdAt) {
        return PaymentOutbox.builder()
                .orderId(approvalReq.orderId())
                .paymentKey(approvalReq.paymentKey())
                .amount(approvalReq.requestedAmount())
                .status(PaymentOutboxStatus.READY)
                .createdAt(createdAt)
                .processingStartedAt(null)
                .publishedAt(null)
                .build();
    }
}
