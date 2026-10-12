package com.v_payment.pay.product.domain.entity;

import com.v_payment.pay.product.entrypoint.dto.msg.QuantityChangeMessage;
import com.v_payment.pay.product.infra.kafka.dto.QuantityChangeResultMessage;
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

@Entity
@Table(
        name = "quantity_change_result_outbox",
        indexes = {
                @Index(
                        name = "idx_quantity_change_result_outbox_status_next_attempt_id",
                        columnList = "status, next_attempt_time, quantity_change_result_outbox_id"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_quantity_change_result_outbox_order_product",
                        columnNames = {"order_code", "product_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuantityChangeResultOutbox {
    @Id
    @Column(name = "quantity_change_result_outbox_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false)
    private String orderCode;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "change_count", nullable = false)
    private Integer changeCount;

    @Column(name = "products_count", nullable = false)
    private Integer productsCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeStatus changeStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QuantityChangeResultOutboxStatus quantityChangeResultOutboxStatus;

    @Column(nullable = false)
    private Integer retryCount;

    private LocalDateTime nextAttemptTime;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private QuantityChangeResultOutbox(
            String orderCode,
            Long productId,
            Integer changeCount,
            Integer productsCount,
            ChangeStatus changeStatus,
            QuantityChangeResultOutboxStatus quantityChangeResultOutboxStatus,
            Integer retryCount,
            LocalDateTime nextAttemptTime,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.orderCode = validateOrderCode(orderCode);
        this.productId = validateProductId(productId);
        this.changeCount = validateChangeCount(changeCount);
        this.productsCount = validateProductsCount(productsCount);
        this.changeStatus = validateChangeStatus(changeStatus);
        this.quantityChangeResultOutboxStatus = validateStatus(quantityChangeResultOutboxStatus);
        this.retryCount = validateRetryCount(retryCount);
        this.nextAttemptTime = nextAttemptTime;
        this.createdAt = validateCreatedAt(createdAt);
        this.updatedAt = updatedAt;
    }

    public static QuantityChangeResultOutbox success(QuantityChangeMessage message, LocalDateTime now) {
        return from(message, ChangeStatus.SUCCESS, now);
    }

    public static QuantityChangeResultOutbox fail(QuantityChangeMessage message, LocalDateTime now) {
        return from(message, ChangeStatus.FAILED, now);
    }

    private static QuantityChangeResultOutbox from(
            QuantityChangeMessage message,
            ChangeStatus changeStatus,
            LocalDateTime now
    ) {
        QuantityChangeMessage validMessage = validateMessage(message);
        return new QuantityChangeResultOutbox(
                validMessage.orderCode(),
                validMessage.productId(),
                validMessage.changeCount(),
                validMessage.productsCount(),
                changeStatus,
                QuantityChangeResultOutboxStatus.READY,
                0,
                null,
                now,
                null
        );
    }

    public void markDone(LocalDateTime updatedAt) {
        this.quantityChangeResultOutboxStatus = QuantityChangeResultOutboxStatus.DONE;
        this.updatedAt = validateUpdatedAt(updatedAt);
    }

    public QuantityChangeResultMessage getQuantityChangeResult() {
        return QuantityChangeResultMessage.of(orderCode, productId, changeCount, changeStatus, productsCount);
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

    private Integer validateChangeCount(Integer changeCount) {
        if (changeCount == null) throw new IllegalArgumentException("재고 변경 수량은 필수입니다.");
        return changeCount;
    }

    private ChangeStatus validateChangeStatus(ChangeStatus changeStatus) {
        if (changeStatus == null) throw new IllegalArgumentException("재고 변경 상태는 필수입니다.");
        return changeStatus;
    }

    private QuantityChangeResultOutboxStatus validateStatus(QuantityChangeResultOutboxStatus status) {
        if (status == null) throw new IllegalArgumentException("결과 아웃박스 상태는 필수입니다.");
        return status;
    }

    private Integer validateRetryCount(Integer retryCount) {
        if (retryCount == null) throw new IllegalArgumentException("재시도 횟수는 필수입니다.");
        if (retryCount < 0) throw new IllegalArgumentException("재시도 횟수는 음수일 수 없습니다.");
        return retryCount;
    }

    private LocalDateTime validateCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null) throw new IllegalArgumentException("생성 시간은 필수입니다.");
        return createdAt;
    }

    private LocalDateTime validateUpdatedAt(LocalDateTime updatedAt) {
        if (updatedAt == null) throw new IllegalArgumentException("수정 시간은 필수입니다.");
        return updatedAt;
    }


    private Integer validateProductsCount(Integer productsCount) {
        if (productsCount == null) throw new IllegalArgumentException("주문 상품 개수는 필수입니다.");
        return productsCount;
    }
}
