package com.v_payment.pay.payment.outbox.entity;

import com.v_payment.pay.payment.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.payment.entity.Payment;
import com.v_payment.pay.payment.payment.infra.FailedResult;
import com.v_payment.pay.payment.payment.infra.PaymentError;
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
                columnList = "status, next_attempt_time, payment_outbox_id"
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

    private Integer attemptCount;

    @Enumerated(EnumType.STRING)
    private PaymentError lastErrorCode;

    private String lastErrorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime nextAttemptTime;

    protected PaymentOutbox() {
    }

    @Builder
    private PaymentOutbox(
            String orderId,
            String paymentKey,
            Long amount,
            PaymentOutboxStatus status,
            Integer attemptCount,
            PaymentError lastErrorCode,
            String lastErrorMessage,
            LocalDateTime createdAt,
            LocalDateTime nextAttemptTime
    ) {
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.amount = amount;
        this.status = status;
        this.attemptCount = attemptCount;
        this.lastErrorCode = lastErrorCode;
        this.lastErrorMessage = lastErrorMessage;
        this.createdAt = createdAt;
        this.nextAttemptTime = nextAttemptTime;
    }

    public PaymentPayload getPaymentPayload() {
        return PaymentPayload.create(orderId, paymentKey, amount);
    }

    public void process() {
        if (status != PaymentOutboxStatus.READY) {
            throw new IllegalStateException("Ready status is required.");
        }
        this.status = PaymentOutboxStatus.PROCESSING;
        this.attemptCount++;
    }

    public void success() {
        if (status != PaymentOutboxStatus.PROCESSING) {
            throw new IllegalStateException("Processing status is required.");
        }
        this.status = PaymentOutboxStatus.PUBLISHED;
    }

    public void failed(FailedResult failedResult, LocalDateTime nextAttemptTime) {
        if (status != PaymentOutboxStatus.PROCESSING) {
            throw new IllegalStateException("Processing status is required.");
        }
        this.status = PaymentOutboxStatus.READY;
        this.lastErrorCode = failedResult.paymentError();
        this.lastErrorMessage = failedResult.message();
        this.nextAttemptTime = nextAttemptTime;
    }

    public void dead(FailedResult failedResult) {
        if (status != PaymentOutboxStatus.PROCESSING) {
            throw new IllegalStateException("Processing status is required.");
        }
        this.status = PaymentOutboxStatus.DEAD;
        this.lastErrorCode = failedResult.paymentError();
        this.lastErrorMessage = failedResult.message();
        this.nextAttemptTime = null;
    }

    public static PaymentOutbox create(Payment payment, LocalDateTime createdAt) {
        return PaymentOutbox.builder()
                .orderId(payment.getOrderId())
                .paymentKey(payment.getPaymentKey())
                .amount(payment.getRequestedAmount())
                .status(PaymentOutboxStatus.READY)
                .attemptCount(0)
                .lastErrorCode(null)
                .lastErrorMessage(null)
                .createdAt(createdAt)
                .nextAttemptTime(createdAt)
                .build();
    }

    public static PaymentOutbox create(ApprovalReq approvalReq, LocalDateTime createdAt) {
        return PaymentOutbox.builder()
                .orderId(approvalReq.orderId())
                .paymentKey(approvalReq.paymentKey())
                .amount(approvalReq.requestedAmount())
                .status(PaymentOutboxStatus.READY)
                .attemptCount(0)
                .lastErrorCode(null)
                .lastErrorMessage(null)
                .createdAt(createdAt)
                .nextAttemptTime(createdAt)
                .build();
    }
}
