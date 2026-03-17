package com.v_payment.pay.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private PaymentMethod method;

    @Embedded
    private PaymentPayload paymentPayload;

    private Long approvedAmount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private LocalDateTime requestedAt;

    private LocalDateTime approvedAt;

    private String receiptUrl;

    public Payment(Provider provider,
                   PaymentMethod method,
                   String orderId,
                   String paymentKey,
                   Long requestedAmount,
                   Long approvedAmount,
                   PaymentStatus paymentStatus,
                   LocalDateTime approvedAt,
                   LocalDateTime requestedAt,
                   String receiptUrl) {
        this.provider = provider;
        this.method = method;
        this.paymentPayload = new PaymentPayload(orderId, paymentKey, requestedAmount);
        this.approvedAmount = approvedAmount;
        this.paymentStatus = paymentStatus;
        this.approvedAt = approvedAt;
        this.requestedAt = requestedAt;
        this.receiptUrl = receiptUrl;
    }

    public PaymentPayload getPaymentPayload() {
        return paymentPayload.toBuilder().build();
    }
}
