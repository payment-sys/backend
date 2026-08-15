package com.v_payment.pay.product.cache.dto;

import java.util.List;

public record WriteAheadLogEntry(
        Long version,
        List<WriteAheadLogItem> items
) {
}
