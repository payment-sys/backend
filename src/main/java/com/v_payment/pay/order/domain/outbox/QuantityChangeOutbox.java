package com.v_payment.pay.order.domain.outbox;

import com.v_payment.pay.order.domain.order.RequestedOrder;
import com.v_payment.pay.order.infrastructure.kafka.dto.QuantityChangeMessage;
import com.v_payment.pay.order.infrastructure.kafka.dto.QuantityChangeType;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Entity
@Table(
        name = "quantity_change_outbox",
        indexes = {
                @Index(name = "idx_quantity_change_outbox_status_next_attempt_id",
                        columnList = "status, next_attempt_time, quantity_change_outbox_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_quantity_change_outbox_order_type",
                        columnNames = {"order_code", "outbox_type"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuantityChangeOutbox {
    @Id
    @Column(name = "quantity_change_outbox_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false)
    private String orderCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    private RequestedOrder requestedOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "outbox_type", nullable = false)
    private QuantityChangeOutboxType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuantityChangeOutboxStatus status;

    @Column(nullable = false)
    private Integer retryCount;

    private LocalDateTime nextAttemptTime;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public QuantityChangeOutbox(String orderCode,
                                RequestedOrder requestedOrder,
                                QuantityChangeOutboxType type,
                                QuantityChangeOutboxStatus status,
                                Integer retryCount,
                                LocalDateTime nextAttemptTime,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt) {
        this.orderCode = validateOrderCode(orderCode);
        this.requestedOrder = validateReqQuantities(requestedOrder);
        this.type = validateType(type);
        this.status = validateStatus(status);
        this.retryCount = validateRetryCount(retryCount);
        this.nextAttemptTime = nextAttemptTime;
        this.createdAt = validateCreatedAt(createdAt);
        this.updatedAt = updatedAt;
    }

    public static QuantityChangeOutbox create(RequestedOrder requestedOrder, Clock clock) {
        return new QuantityChangeOutbox(
                requestedOrder.getOrderCode(),
                requestedOrder,
                QuantityChangeOutboxType.DECREASE,
                QuantityChangeOutboxStatus.READY,
                0,
                null,
                LocalDateTime.now(validateClock(clock)),
                null
        );
    }

    public static QuantityChangeOutbox compensate(String orderCode, Map<Long, Integer> quantitiesByProductId, Clock clock) {
        return new QuantityChangeOutbox(
                orderCode,
                RequestedOrder.ofQuantities(orderCode, quantitiesByProductId),
                QuantityChangeOutboxType.COMPENSATE,
                QuantityChangeOutboxStatus.READY,
                0,
                null,
                LocalDateTime.now(validateClock(clock)),
                null
        );
    }

    public List<QuantityChangeMessage> getQuantityChangeMessages() {
        int size = requestedOrder.getReqQuantities().size();
        return requestedOrder.getQuantityMap()
                .entrySet()
                .stream()
                .map(entry -> new QuantityChangeMessage(orderCode, entry.getKey(),
                        changeCount(entry.getValue()), size, messageType()))
                .toList();
    }

    private QuantityChangeType messageType() {
        if (type == QuantityChangeOutboxType.COMPENSATE) {
            return QuantityChangeType.COMPENSATE;
        }
        return QuantityChangeType.DECREASE;
    }

    private Integer changeCount(Integer quantity) {
        if (type == QuantityChangeOutboxType.DECREASE) {
            return -quantity;
        }
        return quantity;
    }

    private String validateOrderCode(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) throw new IllegalArgumentException("orderCode는 필수입니다.");
        return orderCode;
    }

    private RequestedOrder validateReqQuantities(RequestedOrder requestedOrder) {
        if (requestedOrder == null) throw new IllegalArgumentException("quantityChanges는 필수입니다.");
        return requestedOrder;
    }

    private QuantityChangeOutboxType validateType(QuantityChangeOutboxType type) {
        if (type == null) throw new IllegalArgumentException("type is required.");
        return type;
    }

    private QuantityChangeOutboxStatus validateStatus(QuantityChangeOutboxStatus status) {
        if (status == null) throw new IllegalArgumentException("status는 필수입니다.");
        return status;
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

    private LocalDateTime validateUpdatedAt(LocalDateTime updatedAt) {
        if (updatedAt == null) throw new IllegalArgumentException("updatedAt은 필수입니다.");
        return updatedAt;
    }

    private LocalDateTime validateNextAttemptTime(LocalDateTime nextAttemptTime) {
        if (nextAttemptTime == null) throw new IllegalArgumentException("nextAttemptTime은 필수입니다.");
        return nextAttemptTime;
    }

    private static Clock validateClock(Clock clock) {
        if (clock == null) throw new IllegalArgumentException("clock은 필수입니다.");
        return clock;
    }
}
