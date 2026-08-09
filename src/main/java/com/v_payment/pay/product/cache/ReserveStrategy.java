package com.v_payment.pay.product.cache;

import com.v_payment.pay.product.cache.dto.CachedProduct;
import com.v_payment.pay.product.repository.ProductRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReserveStrategy {
    private final Map<Long, CachedProduct> cachedProducts = new ConcurrentHashMap<>();

    public void loadProducts(ProductCache productCache, ProductRepository productRepository) {

    }
}
