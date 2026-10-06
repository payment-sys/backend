package com.v_payment.pay.order.infra.kafka.dto;

public record QuantityChangeSummaryMessage(
        String orderCode,
        QuantityChangeSummaryStatus status
) {
}
