package com.v_payment.pay.order.infrastructure.kafka.dto;

import java.util.List;
import java.util.Map;

public record QuantityChangeSummaryMessage(
        String orderCode,
        QuantityChangeSummaryStatus status,
        List<Long> successProductIds,
        List<Long> failedProductIds,
        Map<Long, Integer> successQuantities,
        Map<Long, Integer> failedQuantities
) {
    public QuantityChangeSummaryMessage(
            String orderCode,
            QuantityChangeSummaryStatus status,
            List<Long> successProductIds,
            List<Long> failedProductIds
    ) {
        this(orderCode, status, successProductIds, failedProductIds, Map.of(), Map.of());
    }

    public QuantityChangeSummaryMessage {
        successProductIds = successProductIds == null ? List.of() : List.copyOf(successProductIds);
        failedProductIds = failedProductIds == null ? List.of() : List.copyOf(failedProductIds);
        successQuantities = successQuantities == null ? Map.of() : Map.copyOf(successQuantities);
        failedQuantities = failedQuantities == null ? Map.of() : Map.copyOf(failedQuantities);
    }
}
