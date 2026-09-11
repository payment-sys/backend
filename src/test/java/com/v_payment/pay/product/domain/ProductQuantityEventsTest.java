package com.v_payment.pay.product.domain;

import com.v_payment.pay.payment.entity.PaymentMethod;
import com.v_payment.pay.product.domain.entity.ProductQuantityEvent;
import com.v_payment.pay.product.domain.entity.ProductQuantityEventPayload;
import com.v_payment.pay.product.domain.entity.ProductQuantityEventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProductQuantityEventsTest {
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 10, 0, 0);

    @DisplayName("상품 수량 이벤트가 없으면 비어있다")
    @Test
    void isEmptyEvent() {
        // given
        ProductQuantityEvents events = ProductQuantityEvents.from(List.of());

        // when
        boolean empty = events.isEmptyEvent();

        // then
        assertThat(empty).isTrue();
    }

    @DisplayName("상품 수량 이벤트 ID 목록을 꺼낸다")
    @Test
    void getIds() {
        // given
        ProductQuantityEvent first = event(1L, "ORDER-001", Map.of(1L, 2));
        ProductQuantityEvent second = event(2L, "ORDER-002", Map.of(2L, 1));
        ProductQuantityEvents events = ProductQuantityEvents.from(List.of(first, second));

        // when
        List<Long> ids = events.getIds();

        // then
        assertThat(ids).containsExactly(1L, 2L);
    }

    @DisplayName("요청 상품 ID를 중복 없이 꺼낸다")
    @Test
    void getProductIdsDistinct() {
        // given
        ProductQuantityEvent first = event(1L, "ORDER-001", Map.of(1L, 2, 2L, 1));
        ProductQuantityEvent second = event(2L, "ORDER-002", Map.of(1L, 3, 3L, 1));
        ProductQuantityEvents events = ProductQuantityEvents.from(List.of(first, second));

        // when
        List<Long> productIds = events.getProductIdsDistinct();

        // then
        assertThat(productIds).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    private ProductQuantityEvent event(Long id, String orderCode, Map<Long, Integer> requestedQuantities) {
        ProductQuantityEvent event = new ProductQuantityEvent(
                orderCode,
                ProductQuantityEventPayload.of(orderCode, PaymentMethod.CARD, requestedQuantities),
                ProductQuantityEventStatus.READY,
                0,
                null,
                CREATED_AT,
                null
        );
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }
}
