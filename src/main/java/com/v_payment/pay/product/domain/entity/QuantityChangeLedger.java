package com.v_payment.pay.product.domain.entity;

import com.v_payment.pay.product.entrypoint.dto.msg.QuantityChangeMessage;
import com.v_payment.pay.product.entrypoint.dto.msg.QuantityChangeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "quantity_change_ledger",
        indexes = {
                @Index(
                        name = "idx_quantity_change_ledger_product_created",
                        columnList = "product_id, created_at"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_quantity_change_ledger_idempotency",
                        columnNames = {"order_code", "product_id", "change_type"}
                )
        }
)
public class QuantityChangeLedger {
    @Id
    @Column(name = "quantity_change_ledger_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false)
    private String orderCode;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private QuantityChangeType changeType;

    @Column(name = "change_count", nullable = false)
    private Integer changeCount;

    @Column(name = "stock_before")
    private Integer stockBefore;

    @Column(name = "stock_after")
    private Integer stockAfter;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private QuantityChangeLedger(
            String orderCode,
            Long productId,
            QuantityChangeType changeType,
            Integer changeCount,
            Integer stockBefore,
            Integer stockAfter,
            LocalDateTime createdAt
    ) {
        this.orderCode = validateOrderCode(orderCode);
        this.productId = validateProductId(productId);
        this.changeType = validateChangeType(changeType);
        this.changeCount = validateChangeCount(changeCount);
        this.stockBefore = stockBefore;
        this.stockAfter = stockAfter;
        this.createdAt = validateCreatedAt(createdAt);
    }

    public static QuantityChangeLedger success(
            QuantityChangeMessage message,
            Integer stockBefore,
            Integer stockAfter,
            LocalDateTime now
    ) {
        QuantityChangeMessage validMessage = validateMessage(message);
        validateSuccessStock(stockBefore, stockAfter);
        return new QuantityChangeLedger(
                validMessage.orderCode(),
                validMessage.productId(),
                validMessage.type(),
                validMessage.changeCount(),
                stockBefore,
                stockAfter,
                now
        );
    }

    public static QuantityChangeLedger success(
            QuantityChangeMessage message,
            LocalDateTime now
    ) {
        QuantityChangeMessage validMessage = validateMessage(message);
        return new QuantityChangeLedger(
                validMessage.orderCode(),
                validMessage.productId(),
                validMessage.type(),
                validMessage.changeCount(),
                null,
                null,
                now
        );
    }

    private static QuantityChangeMessage validateMessage(QuantityChangeMessage message) {
        if (message == null) throw new IllegalArgumentException("재고 변경 메시지는 필수입니다.");
        return message;
    }

    private String validateOrderCode(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) throw new IllegalArgumentException("주문 코드는 필수입니다.");
        return orderCode;
    }

    private Long validateProductId(Long productId) {
        if (productId == null) throw new IllegalArgumentException("상품 ID는 필수입니다.");
        return productId;
    }

    private QuantityChangeType validateChangeType(QuantityChangeType changeType) {
        if (changeType == null) throw new IllegalArgumentException("재고 변경 타입은 필수입니다.");
        return changeType;
    }

    private Integer validateChangeCount(Integer changeCount) {
        if (changeCount == null) throw new IllegalArgumentException("재고 변경 수량은 필수입니다.");
        if (changeCount == 0) throw new IllegalArgumentException("재고 변경 수량은 0일 수 없습니다.");
        return changeCount;
    }

    private static void validateSuccessStock(Integer stockBefore, Integer stockAfter) {
        if (stockBefore == null) throw new IllegalArgumentException("변경 전 재고는 필수입니다.");
        if (stockAfter == null) throw new IllegalArgumentException("변경 후 재고는 필수입니다.");
        if (stockAfter < 0) throw new IllegalArgumentException("변경 후 재고는 음수일 수 없습니다.");
    }

    private LocalDateTime validateCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null) throw new IllegalArgumentException("생성 시간은 필수입니다.");
        return createdAt;
    }
}
