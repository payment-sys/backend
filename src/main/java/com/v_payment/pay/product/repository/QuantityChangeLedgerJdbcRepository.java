package com.v_payment.pay.product.repository;

import com.v_payment.pay.product.domain.entity.QuantityChangeLedger;
import com.v_payment.pay.product.entrypoint.dto.msg.QuantityChangeMessage;

import java.util.List;

public interface QuantityChangeLedgerJdbcRepository {
    void createLedgerBatch(List<QuantityChangeLedger> ledgers);

    List<QuantityChangeLedger> findExistingLedgers(List<QuantityChangeMessage> messages);
}
