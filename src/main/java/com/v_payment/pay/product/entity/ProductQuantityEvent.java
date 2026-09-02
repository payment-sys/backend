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
        this.orderCode = orderCode;
        this.payload = payload;
        this.productQuantityEventStatus = productQuantityEventStatus;
        this.retryCount = retryCount;
        this.nextAttemptTime = nextAttemptTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void markConsumed(Clock clock) {
        this.productQuantityEventStatus = ProductQuantityEventStatus.CONSUMED;
        this.updatedAt = LocalDateTime.now(clock);
    }

    public void markRetry(Clock clock) {
        this.productQuantityEventStatus = ProductQuantityEventStatus.RETRY;
        this.retryCount++;
        this.updatedAt = LocalDateTime.now(clock);
    }

    public static ProductQuantityEvent of(String orderCode, ProductQuantityEventPayload payload, Clock clock) {
        return new ProductQuantityEvent(orderCode, payload, ProductQuantityEventStatus.READY, 0, null,
                LocalDateTime.now(clock), null);
    }
}
