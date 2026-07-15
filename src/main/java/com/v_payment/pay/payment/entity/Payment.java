package com.v_payment.pay.payment.entity;

import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.SuccessResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;

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

    private String failedMessage;

    @Version
    private Integer version;

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
                   String receiptUrl) {
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
    }

    public PaymentPayload getPaymentPayload() {
        return PaymentPayload.create(orderCode, paymentKey, requestedAmount);
    }

    public boolean isSameRequestedAmount(Long requestedAmount) {
        return this.requestedAmount.equals(requestedAmount);
    }

    public boolean isSameMethod(PaymentMethod paymentMethod) {
        return this.paymentMethod == paymentMethod;
    }

    public boolean isSameProvider(Provider provider) {
        return this.provider.equals(provider);
    }

    public void completeValidate(ApprovalReq approvalReq) {
        this.paymentStatus = PaymentStatus.APPROVING;
        this.paymentKey = approvalReq.paymentKey();
    }

    public void success(SuccessResult successResult) {
        this.paymentStatus = PaymentStatus.APPROVED;
        this.approvedAmount = successResult.totalAmount();
        this.approvedAt = successResult.approvedAt();
        this.receiptUrl = successResult.receipt().url();
    }

    public void failed(FailedResult failedResult) {
        this.failedMessage = failedResult.message();
        this.paymentStatus = PaymentStatus.REJECTED;
    }

    public void retryFailed() {
        this.failedMessage = "retryFailed : unknown error";
        this.paymentStatus = PaymentStatus.REJECTED;
    }

    public static Payment createForOrder(String orderCode, Long amount, PaymentMethod paymentMethod, Clock clock) {
        return Payment.builder()
                .provider(Provider.TOSS)
                .paymentMethod(paymentMethod)
                .orderCode(orderCode)
                .paymentKey(null)
                .requestedAmount(amount)
                .approvedAmount(null)
                .paymentStatus(PaymentStatus.PENDING)
                .requestedAt(LocalDateTime.now(clock))
                .approvedAt(null)
                .receiptUrl(null)
                .build();
    }
}
