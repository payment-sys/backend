package com.v_payment.pay.product.cache;

public interface CacheLoadPolicy {
    int getLoadQuantityCount(int currCachedQuantity, int dbRemainQuantity, int requestedQuantity);
}
