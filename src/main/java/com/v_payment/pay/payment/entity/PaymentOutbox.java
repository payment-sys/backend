package com.v_payment.pay.payment.entity;

import com.v_payment.pay.payment.infra.PaymentError;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "payment_outbox")
public class PaymentOutbox {
    private static final int MAX_ATTEMPT_COUNT = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_outbox_id")
    private Long id;

    private String orderId;

    private String paymentKey;

    private Long amount;

    @Enumerated(EnumType.STRING)
    private PaymentOutboxStatus status;

    @Version
    private Integer attemptCount;

    private String lastErrorCode;

    private String lastErrorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime nextAttemptTime;

    protected PaymentOutbox() {
    }

    @Builder
    private PaymentOutbox(String orderId,
                          String paymentKey,
                          Long amount,
                          PaymentOutboxStatus status,
                          Integer attemptCount,
                          String lastErrorCode,
                          String lastErrorMessage,
                          LocalDateTime createdAt,
                          LocalDateTime nextAttemptTime) {
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

    public void updateStatus(PaymentOutboxStatus status) {
        this.status = status;
    }

    public void plusAttemptCount() {
        this.attemptCount++;
    }

    public void updateLastErrorCode(String lastErrorCode) {
        this.lastErrorCode = lastErrorCode;
    }

    public void updateLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public void updateNextAttemptTime(LocalDateTime nextAttemptTime) {
        this.nextAttemptTime = nextAttemptTime;
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
}
