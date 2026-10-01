package com.v_payment.pay.product.repository;

import com.v_payment.pay.product.domain.entity.QuantityChangeResultOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuantityChangeResultOutboxRepository extends JpaRepository<QuantityChangeResultOutbox, Long>, QuantityChangeResultOutboxJdbcRepository {
}
