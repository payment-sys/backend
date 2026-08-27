package com.v_payment.pay.product.repository;

import com.v_payment.pay.product.entity.ProductQuantityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductQuantityEventRepository extends JpaRepository<ProductQuantityEvent, Long> {
}
