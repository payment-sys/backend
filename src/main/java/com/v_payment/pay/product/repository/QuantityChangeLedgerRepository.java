package com.v_payment.pay.product.repository;

import com.v_payment.pay.product.domain.entity.QuantityChangeLedger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuantityChangeLedgerRepository extends JpaRepository<QuantityChangeLedger, Long>, QuantityChangeLedgerJdbcRepository {
}
