package com.v_payment.pay.product.cache;

import com.v_payment.pay.product.service.ProductManager;
import com.v_payment.pay.product.service.ReservedProduct;

import java.time.Duration;
import java.util.List;

public interface ProductCache {

    List<ReservedProduct> reserve(List<ProductManager.ProductReservationReq> requests);

    void restore(List<ProductManager.ProductRestoreReq> requests);

    int restoreStaleProducts(Duration staleAfter);
}
