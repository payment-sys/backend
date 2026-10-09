package com.v_payment.pay.order.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.v_payment.pay.order.infrastructure.kafka.dto.QuantityChangeResultMessage;
import com.v_payment.pay.order.infrastructure.kafka.dto.QuantityChangeSummaryStatus;
import com.v_payment.pay.product.domain.entity.ChangeStatus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public class QuantityChangeResultAggregationState {
    private String orderCode;
    private Integer productsCount;
    private Set<Long> receivedProductIds = new HashSet<>();
    private Set<Long> successProductIds = new HashSet<>();
    private Set<Long> failedProductIds = new HashSet<>();
    private Map<Long, Integer> successQuantities = new HashMap<>();
    private Map<Long, Integer> failedQuantities = new HashMap<>();
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

    public List<Long> successProductIds() {
        return successProductIds.stream().sorted().toList();
    }

    public List<Long> failedProductIds() {
        return failedProductIds.stream().sorted().toList();
    }

    public Map<Long, Integer> successQuantities() {
        return sortedQuantities(successQuantities);
    }

    public Map<Long, Integer> failedQuantities() {
        return sortedQuantities(failedQuantities);
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
            failedProductIds.add(message.productId());
            failedQuantities.merge(message.productId(), positiveQuantity(message.changeCount()), Integer::sum);
            hasFailure = true;
        } else {
            successProductIds.add(message.productId());
            successQuantities.merge(message.productId(), positiveQuantity(message.changeCount()), Integer::sum);
        }
        return this;
    }

    private Integer positiveQuantity(Integer changeCount) {
        if (changeCount == null) return 0;
        return Math.abs(changeCount);
    }

    private Map<Long, Integer> sortedQuantities(Map<Long, Integer> quantities) {
        return quantities.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        Integer::sum,
                        java.util.LinkedHashMap::new
                ));
    }
}
