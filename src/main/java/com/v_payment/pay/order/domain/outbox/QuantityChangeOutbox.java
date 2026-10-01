package com.v_payment.pay.order.domain.outbox;

import com.v_payment.pay.order.domain.ReqQuantities;
import com.v_payment.pay.order.domain.ReqQuantity;
import com.v_payment.pay.order.infra.dto.QuantityChangeMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Entity
@Table(
        name = "quantity_change_outbox",
        indexes = {
                @Index(name = "idx_quantity_change_outbox_status_next_attempt_id",
                        columnList = "status, next_attempt_time, quantity_change_outbox_id")
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
    private ReqQuantities reqQuantities;

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
                                ReqQuantities reqQuantities,
                                QuantityChangeOutboxStatus status,
                                Integer retryCount,
                                LocalDateTime nextAttemptTime,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt) {
        this.orderCode = validateOrderCode(orderCode);
        this.reqQuantities = validateReqQuantities(reqQuantities);
        this.status = validateStatus(status);
        this.retryCount = validateRetryCount(retryCount);
        this.nextAttemptTime = nextAttemptTime;
        this.createdAt = validateCreatedAt(createdAt);
        this.updatedAt = updatedAt;
    }

    public static QuantityChangeOutbox of(ReqQuantities reqQuantities, Clock clock) {
        return new QuantityChangeOutbox(
                reqQuantities.getOrderCode(),
                reqQuantities,
                QuantityChangeOutboxStatus.READY,
                0,
                null,
                LocalDateTime.now(validateClock(clock)),
                null
        );
    }

    public List<CompletableFuture<?>> publishEach(QuantityChangeMessagePublisher publisher) {
        List<CompletableFuture<?>> results = new ArrayList<>();
        for (ReqQuantity reqQuantity : reqQuantities.getReqQuantities()) {
            CompletableFuture<?> publishResult = publisher.publish(orderCode, reqQuantity.getProductId(),
                    reqQuantity.getQuantity());

            results.add(publishResult);
        }
        return results;
    }

    public List<QuantityChangeMessage> getQuantityChangeMessages() {
        return reqQuantities.getQuantityMap()
                .entrySet()
                .stream()
                .map(entry -> new QuantityChangeMessage(
                        orderCode,
                        entry.getKey(),
                        entry.getValue()
                ))
                .toList();
    }

    public void markDone(LocalDateTime updatedAt) {
        this.status = QuantityChangeOutboxStatus.DONE;
        this.updatedAt = validateUpdatedAt(updatedAt);
    }

    public Long getId() {
        return id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public ReqQuantities getReqQuantities() {
        return reqQuantities;
    }

    public QuantityChangeOutboxStatus getStatus() {
        return status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public LocalDateTime getNextAttemptTime() {
        return nextAttemptTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @FunctionalInterface
    public interface QuantityChangeMessagePublisher {
        CompletableFuture<?> publish(String orderCode, Long productId, Integer changeCount);
    }

    private String validateOrderCode(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) throw new IllegalArgumentException("orderCode는 필수입니다.");
        return orderCode;
    }

    private ReqQuantities validateReqQuantities(ReqQuantities reqQuantities) {
        if (reqQuantities == null) throw new IllegalArgumentException("quantityChanges는 필수입니다.");
        return reqQuantities;
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
