package com.v_payment.pay.product.cache;

import org.springframework.stereotype.Component;

@Component
public class HalfLoadPolicy implements CacheLoadPolicy {
    private static final int MIN_LOAD_QUANTITY = 100;
    private static final int MAX_LOAD_QUANTITY = 5000;

    @Override
    public int getLoadQuantityCount(int currCachedQuantity, int dbRemainQuantity, int requestedQuantity) {
        int shortage = requestedQuantity - currCachedQuantity;
        if (shortage <= 0) return 0;

        if (dbRemainQuantity <= shortage) return dbRemainQuantity;

        int half = Math.min(dbRemainQuantity / 2, MAX_LOAD_QUANTITY);
        if (half < MIN_LOAD_QUANTITY) return dbRemainQuantity;

        return Math.max(half, shortage);
    }
}
