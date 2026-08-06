package com.v_payment.pay.product.service;

import com.v_payment.pay.product.cache.ProductCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductManager {
    private final ProductCache productCache;

    public List<ReservedProduct> reserve(List<ProductReservationReq> requests) {
        return productCache.reserve(requests);
    }

    public void restore(List<ProductRestoreReq> requests) {
        productCache.restore(requests);
    }

    public record ProductReservationReq(
            Long productId,
            Integer quantity
    ) {
    }

    public record ProductRestoreReq(
            Long productId,
            Integer quantity
    ) {
    }
}
