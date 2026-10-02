package com.v_payment.pay.product.repository;

import java.util.Map;

public interface ProductJdbcRepository {
    void changeQuantityBatch(Map<Long, Integer> successChangeQuantities);
}
