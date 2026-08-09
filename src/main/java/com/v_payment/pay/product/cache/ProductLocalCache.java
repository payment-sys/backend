package com.v_payment.pay.product.cache;

import com.v_payment.pay.product.cache.dto.CachedProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ProductLocalCache implements ProductCache {
    private final Map<Long, CachedProduct> productCache = new ConcurrentHashMap<>();
    private final Clock clock;

    @Override
    public Map<Long, CachedProduct> findAll(List<Long> productIds) {
        Map<Long, CachedProduct> cachedProducts = new LinkedHashMap<>();
        for (Long productId : productIds) {
            CachedProduct product = productCache.get(productId);
            if (product != null) cachedProducts.put(productId, product);
        }
        return cachedProducts;
    }

    @Override
    public void put(Long productId, CachedProduct productForNewCaching) {
        productCache.put(productId, productForNewCaching);
    }
}
