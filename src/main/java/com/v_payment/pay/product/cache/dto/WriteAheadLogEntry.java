package com.v_payment.pay.product.cache.dto;

import java.util.List;

public record WriteAheadLogEntry(
        Long version,
        String type,
        List<WriteAheadLogItem> items
) {
}
