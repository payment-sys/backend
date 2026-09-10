package com.v_payment.pay.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "product_quantity_event",
        indexes = {
                @Index(name = "idx_pqe_status_id",
                        columnList = "status, product_quantity_event_id"),
                @Index(name = "idx_product_quantity_event_status_next_attempt_id",
                        columnList = "status, next_attempt_time, product_quantity_event_id")
        }
)
public class ProductQuantityEvent {
    @Id
    @Column(name = "product_quantity_event_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private String orderCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private ProductQuantityEventPayload payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductQuantityEventStatus productQuantityEventStatus;

    private Integer retryCount;

    private LocalDateTime nextAttemptTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public ProductQuantityEvent(String orderCode,
                                ProductQuantityEventPayload payload,
                                ProductQuantityEventStatus productQuantityEventStatus,
                                Integer retryCount,
                                LocalDateTime nextAttemptTime,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt) {
        this.orderCode = validateOrderCode(orderCode);
        this.payload = validatePayload(payload);
        this.productQuantityEventStatus = validateProductQuantityEventStatus(productQuantityEventStatus);
        this.retryCount = validateRetryCount(retryCount);
        this.nextAttemptTime = nextAttemptTime;
        this.createdAt = validateCreatedAt(createdAt);
        this.updatedAt = updatedAt;
    }

    public static ProductQuantityEvent of(String orderCode, ProductQuantityEventPayload payload, Clock clock) {
        return new ProductQuantityEvent(orderCode, payload, ProductQuantityEventStatus.READY, 0, null,
                LocalDateTime.now(clock), null);
    }

    private String validateOrderCode(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) throw new IllegalArgumentException("orderCode는 필수입니다.");
        return orderCode;
    }

    private ProductQuantityEventPayload validatePayload(ProductQuantityEventPayload payload) {
        if (payload == null) throw new IllegalArgumentException("payload는 필수입니다.");
        return payload;
    }

    private ProductQuantityEventStatus validateProductQuantityEventStatus(
            ProductQuantityEventStatus productQuantityEventStatus
    ) {
        if (productQuantityEventStatus == null) {
            throw new IllegalArgumentException("productQuantityEventStatus는 필수입니다.");
        }
        return productQuantityEventStatus;
    }

    private Integer validateRetryCount(Integer retryCount) {
        if (retryCount == null) throw new IllegalArgumentException("retryCount는 필수입니다.");
        if (retryCount < 0) throw new IllegalArgumentException("retryCount는 음수일 수 없습니다.");
        return retryCount;
    }

    private LocalDateTime validateCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null) throw new IllegalArgumentException("createdAt은 필수입니다.");
        return createdAt;
    }
}
