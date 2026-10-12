package com.v_payment.pay.order.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.v_payment.pay.order.domain.order.OrderItemInfo;
import com.v_payment.pay.order.domain.order.OrderItemStatus;
import com.v_payment.pay.order.infrastructure.kafka.dto.QuantityChangeResultMessage;
import com.v_payment.pay.order.infrastructure.kafka.dto.QuantityChangeSummaryStatus;
import com.v_payment.pay.product.domain.entity.ChangeStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public class QuantityChangeResultAggregationState {
    private String orderCode;
    private Integer productsCount;
    private Set<Long> receivedProductIds = new HashSet<>();
    private Map<Long, OrderItemInfo> orderItems = new HashMap<>();
    private boolean hasFailure;
    private boolean emitted;

    public String orderCode() {
        return orderCode;
    }

    public boolean isCompleted() {
        return productsCount != null && receivedProductIds.size() >= productsCount;
    }

    public boolean isEmitted() {
        return emitted;
    }

    public QuantityChangeSummaryStatus summaryStatus() {
        if (hasFailure) return QuantityChangeSummaryStatus.FAILED;
        return QuantityChangeSummaryStatus.SUCCESS;
    }

    public List<OrderItemInfo> orderItems() {
        return orderItems.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
    }

    public void markEmitted() {
        this.emitted = true;
    }

    public QuantityChangeResultAggregationState add(QuantityChangeResultMessage message) {
        if (message == null) return this;
        if (orderCode == null) orderCode = message.orderCode();
        if (productsCount == null) productsCount = message.productsCount();
        if (!receivedProductIds.add(message.productId())) {
            return this;
        }
        if (message.changeStatus() == ChangeStatus.FAILED) {
            addOrderItem(message, OrderItemStatus.FAILED);
            hasFailure = true;
        } else {
            addOrderItem(message, OrderItemStatus.CHANGED);
        }
        return this;
    }

    private void addOrderItem(QuantityChangeResultMessage message, OrderItemStatus status) {
        if (message.productName() == null || message.unitPrice() == null) {
            log.error("Product name and unit은 null일 수 없습니다. orderCode={}", orderCode);
            throw new IllegalArgumentException("Product name and unit은 null일 수 없습니다.");
        }
        orderItems.put(message.productId(), OrderItemInfo.create(
                message.productId(),
                message.productName(),
                message.unitPrice(),
                positiveQuantity(message.changeCount()),
                status
        ));
    }

    private Integer positiveQuantity(Integer changeCount) {
        if (changeCount == null) return 0;
        return Math.abs(changeCount);
    }

}
