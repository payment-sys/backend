package com.v_payment.pay.product.domain;

import com.v_payment.pay.payment.entity.PaymentMethod;
import com.v_payment.pay.product.domain.entity.Product;
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

class QuantityDecreasePlannerTest {
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 10, 0, 0);

    @DisplayName("재고가 충분한 이벤트는 성공 이벤트와 차감 계획에 포함된다")
    @Test
    void makeDecreasePlanWithSuccessEvent() {
        // given
        ProductQuantityEvent event = event(1L, "ORDER-001", Map.of(1L, 2, 2L, 1));
        QuantityDecreasePlanner planner = planner(Map.of(
                1L, product(1L, 10_000L, 3),
                2L, product(2L, 20_000L, 2)
        ));

        // when
        QuantityDecreasePlan plan = planner.makeDecreasePlan(ProductQuantityEvents.from(List.of(event)));

        // then
        assertThat(plan.getSuccess()).containsExactly(event);
        assertThat(plan.hasFailOrder()).isFalse();
        assertThat(plan.getDecreaseTotal()).containsExactlyInAnyOrderEntriesOf(Map.of(
                1L, -2,
                2L, -1
        ));
    }

    @DisplayName("재고가 부족한 이벤트는 실패 이벤트에 포함되고 차감 계획에는 포함되지 않는다")
    @Test
    void makeDecreasePlanWithFailureEvent() {
        // given
        ProductQuantityEvent event = event(1L, "ORDER-001", Map.of(1L, 4));
        QuantityDecreasePlanner planner = planner(Map.of(
                1L, product(1L, 10_000L, 3)
        ));

        // when
        QuantityDecreasePlan plan = planner.makeDecreasePlan(ProductQuantityEvents.from(List.of(event)));

        // then
        assertThat(plan.getSuccess()).isEmpty();
        assertThat(plan.hasFailOrder()).isTrue();
        assertThat(plan.getFailOrderCodes()).containsExactly("ORDER-001");
        assertThat(plan.getDecreaseTotal()).isEmpty();
    }

    @DisplayName("앞선 성공 이벤트의 차감 수량을 반영해서 다음 이벤트 성공 여부를 판단한다")
    @Test
    void makeDecreasePlanWithAccumulatedDecrease() {
        // given
        ProductQuantityEvent first = event(1L, "ORDER-001", Map.of(1L, 3));
        ProductQuantityEvent second = event(2L, "ORDER-002", Map.of(1L, 2));
        QuantityDecreasePlanner planner = planner(Map.of(
                1L, product(1L, 10_000L, 4)
        ));

        // when
        QuantityDecreasePlan plan = planner.makeDecreasePlan(ProductQuantityEvents.from(List.of(first, second)));

        // then
        assertThat(plan.getSuccess()).containsExactly(first);
        assertThat(plan.getFailOrderCodes()).containsExactly("ORDER-002");
        assertThat(plan.getDecreaseTotal()).containsExactlyEntriesOf(Map.of(1L, -3));
    }

    @DisplayName("여러 성공 이벤트의 차감 수량을 상품별로 합산한다")
    @Test
    void makeDecreasePlanMergesDecreaseQuantityByProduct() {
        // given
        ProductQuantityEvent first = event(1L, "ORDER-001", Map.of(1L, 2));
        ProductQuantityEvent second = event(2L, "ORDER-002", Map.of(1L, 3));
        QuantityDecreasePlanner planner = planner(Map.of(
                1L, product(1L, 10_000L, 5)
        ));

        // when
        QuantityDecreasePlan plan = planner.makeDecreasePlan(ProductQuantityEvents.from(List.of(first, second)));

        // then
        assertThat(plan.getSuccess()).containsExactly(first, second);
        assertThat(plan.getDecreaseTotal()).containsExactlyEntriesOf(Map.of(1L, -5));
    }

    private QuantityDecreasePlanner planner(Map<Long, Product> products) {
        return QuantityDecreasePlanner.from(ProductSnapShots.from(products));
    }

    private Product product(Long id, Long price, Integer stockQuantity) {
        Product product = Product.create("product-" + id, price, stockQuantity);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
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
