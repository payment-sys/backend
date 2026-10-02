package com.v_payment.pay.order.repository;

import com.v_payment.pay.order.domain.orderitem.OrderItemStatus;
import com.v_payment.pay.order.infra.kafka.dto.QuantityChangeResultMessage;

import java.util.List;

public interface OrderItemJdbcRepository {
    void updateStatusByQuantityChangeResults(
            List<QuantityChangeResultMessage> results,
            OrderItemStatus processingStatus,
            OrderItemStatus successStatus,
            OrderItemStatus failStatus
    );
}
