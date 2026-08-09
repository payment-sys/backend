package com.v_payment.pay.product.cache;

import com.v_payment.pay.product.cache.dto.CachedProduct;

import java.util.List;
import java.util.Map;

public interface ProductCache {
    Map<Long, CachedProduct> findAll(List<Long> productIds);

    void put(Long productId, CachedProduct newCachingProduct);
}
