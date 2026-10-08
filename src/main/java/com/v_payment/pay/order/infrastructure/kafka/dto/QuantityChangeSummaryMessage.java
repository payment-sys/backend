package com.v_payment.pay.order.infra.kafka.dto;

import java.util.List;

public record QuantityChangeSummaryMessage(
        String orderCode,
        QuantityChangeSummaryStatus status,
        List<Long> successProductIds,
        List<Long> failedProductIds
) {
    public QuantityChangeSummaryMessage {
        successProductIds = successProductIds == null ? List.of() : List.copyOf(successProductIds);
        failedProductIds = failedProductIds == null ? List.of() : List.copyOf(failedProductIds);
    }
}
