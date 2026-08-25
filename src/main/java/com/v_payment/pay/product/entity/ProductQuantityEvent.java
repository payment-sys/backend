package com.v_payment.pay.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "product_quantity_event")
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
    private ProductQuantityEventStatus productQuantityEventStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public ProductQuantityEvent(String orderCode,
                                ProductQuantityEventPayload payload,
                                ProductQuantityEventStatus productQuantityEventStatus,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt) {
        this.orderCode = orderCode;
        this.payload = payload;
        this.productQuantityEventStatus = productQuantityEventStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProductQuantityEvent of(String orderCode, ProductQuantityEventPayload payload, Clock clock) {
        return new ProductQuantityEvent(orderCode, payload, ProductQuantityEventStatus.READY, LocalDateTime.now(clock),
                null);
    }
}
