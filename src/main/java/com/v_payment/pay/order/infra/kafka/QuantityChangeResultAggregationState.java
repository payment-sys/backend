package com.v_payment.pay.order.infra.kafka;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.v_payment.pay.order.infra.kafka.dto.QuantityChangeResultMessage;
import com.v_payment.pay.order.infra.kafka.dto.QuantityChangeSummaryStatus;
import com.v_payment.pay.product.domain.entity.ChangeStatus;

import java.util.HashSet;
import java.util.Set;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public class QuantityChangeResultAggregationState {
    private String orderCode;
    private Integer productsCount;
    private Set<Long> receivedProductIds = new HashSet<>();
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

    public void markEmitted() {
        this.emitted = true;
    }

    public QuantityChangeResultAggregationState add(QuantityChangeResultMessage message) {
        if (message == null) return this;
        if (orderCode == null) orderCode = message.orderCode();
        if (productsCount == null) productsCount = message.productsCount();
        receivedProductIds.add(message.productId());
        if (message.changeStatus() == ChangeStatus.FAILED) hasFailure = true;
        return this;
    }
}
