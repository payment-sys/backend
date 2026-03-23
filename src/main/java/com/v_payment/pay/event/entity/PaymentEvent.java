package com.v_payment.pay.event.entity;

import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.infra.SuccessResult;
import com.v_payment.pay.payment.infra.SuccessResult.Receipt;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "payment_event")
public class PaymentEvent {
    @Id
    @Column(name = "payment_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    private String orderId;

    private String paymentKey;

    private Long requestedAmount;

    @Enumerated(EnumType.STRING)
    private PaymentEventStatus paymentEventStatus;

    private LocalDateTime nextRetryTime;

    private Integer retryCount;

    private String failureCode;

    private String failureMessage;

    private LocalDateTime lastFailedAt;

    @Builder
    public PaymentEvent(
        Payment payment,
        String orderId,
        String paymentKey,
        Long requestedAmount,
        PaymentEventStatus paymentEventStatus,
        LocalDateTime nextRetryTime,
        Integer retryCount,
        String failureCode,
        String failureMessage,
        LocalDateTime lastFailedAt) {
        this.payment = payment;
        this.orderId = orderId;
        this.paymentKey = paymentKey;
        this.requestedAmount = requestedAmount;
        this.paymentEventStatus = paymentEventStatus;
        this.nextRetryTime = nextRetryTime;
        this.retryCount = retryCount;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.lastFailedAt = lastFailedAt;
    }

    public PaymentPayload getPaymentPayload() {
        return PaymentPayload.create(orderId, paymentKey, requestedAmount);
    }

    public static PaymentEvent create(PaymentPayload paymentPayload, final Payment payment) {
        return PaymentEvent.builder()
            .payment(payment)
            .orderId(paymentPayload.getOrderId())
            .paymentKey(paymentPayload.getPaymentKey())
            .paymentEventStatus(PaymentEventStatus.PENDING)
            .requestedAmount(paymentPayload.getRequestedAmount())
            .nextRetryTime(null)
            .retryCount(0)
            .failureCode(null)
            .failureMessage(null)
            .lastFailedAt(null)
            .build();
    }

    public void success() {
        this.paymentEventStatus = PaymentEventStatus.APPROVED;
    }

    public void failed() {
        this.paymentEventStatus = PaymentEventStatus.FAILED;
    }

    public void retry() {
        this.paymentEventStatus = PaymentEventStatus.RETRYABLE;
    }
}