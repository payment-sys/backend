package com.v_payment.pay.order.infrastructure.kafka;

import com.v_payment.pay.order.infrastructure.kafka.dto.QuantityChangeResultMessage;
import com.v_payment.pay.product.domain.entity.ChangeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantityChangeResultAggregationStateTest {

    @DisplayName("상품 스냅샷이 없는 재고 변경 결과는 집계할 수 없다")
    @Test
    void addWithoutProductSnapshot() {
        QuantityChangeResultAggregationState state = new QuantityChangeResultAggregationState();

        QuantityChangeResultMessage message = new QuantityChangeResultMessage(
                "order-code",
                1L,
                null,
                10_000L,
                -1,
                ChangeStatus.SUCCESS,
                1
        );

        assertThatThrownBy(() -> state.add(message))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
