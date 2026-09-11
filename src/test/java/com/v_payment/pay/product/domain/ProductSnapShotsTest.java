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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductSnapShotsTest {
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 10, 0, 0);

    @DisplayName("상품 스냅샷은 남은 수량만큼 차감할 수 있다")
    @Test
    void productSnapshotCanDecrease() {
        // given
        ProductSnapShot productSnapShot = new ProductSnapShot(3);

        // when
        boolean canDecrease = productSnapShot.canDecrease(3);

        // then
        assertThat(canDecrease).isTrue();
    }

    @DisplayName("상품 스냅샷은 남은 수량보다 많이 차감할 수 없다")
    @Test
    void productSnapshotCannotDecreaseOverRemainQuantity() {
        // given
        ProductSnapShot productSnapShot = new ProductSnapShot(3);

        // when
        boolean canDecrease = productSnapShot.canDecrease(4);

        // then
        assertThat(canDecrease).isFalse();
    }

    @DisplayName("상품 스냅샷은 남은 수량이 부족하면 차감에 실패한다")
    @Test
    void productSnapshotDecreaseWithOverRemainQuantity() {
        // given
        ProductSnapShot productSnapShot = new ProductSnapShot(3);

        // when & then
        assertThatThrownBy(() -> productSnapShot.decrease(4))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("상품 스냅샷 목록은 이벤트의 모든 요청 수량이 충분하면 성공으로 판단한다")
    @Test
    void isEventSuccess() {
        // given
        ProductSnapShots productSnapShots = ProductSnapShots.from(Map.of(
                1L, product(1L, 10_000L, 3),
                2L, product(2L, 20_000L, 2)
        ));
        ProductQuantityEvent event = event("ORDER-001", Map.of(1L, 2, 2L, 1));

        // when
        boolean success = productSnapShots.isEventSuccess(event);

        // then
        assertThat(success).isTrue();
    }

    @DisplayName("상품 스냅샷 목록은 요청 상품이 없으면 실패로 판단한다")
    @Test
    void isEventSuccessWithMissingProduct() {
        // given
        ProductSnapShots productSnapShots = ProductSnapShots.from(Map.of(
                1L, product(1L, 10_000L, 3)
        ));
        ProductQuantityEvent event = event("ORDER-001", Map.of(1L, 2, 2L, 1));

        // when
        boolean success = productSnapShots.isEventSuccess(event);

        // then
        assertThat(success).isFalse();
    }

    @DisplayName("상품 스냅샷 목록은 요청 수량이 부족하면 실패로 판단한다")
    @Test
    void isEventSuccessWithInsufficientQuantity() {
        // given
        ProductSnapShots productSnapShots = ProductSnapShots.from(Map.of(
                1L, product(1L, 10_000L, 1)
        ));
        ProductQuantityEvent event = event("ORDER-001", Map.of(1L, 2));

        // when
        boolean success = productSnapShots.isEventSuccess(event);

        // then
        assertThat(success).isFalse();
    }

    private Product product(Long id, Long price, Integer stockQuantity) {
        Product product = Product.create("product-" + id, price, stockQuantity);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private ProductQuantityEvent event(String orderCode, Map<Long, Integer> requestedQuantities) {
        return new ProductQuantityEvent(
                orderCode,
                ProductQuantityEventPayload.of(orderCode, PaymentMethod.CARD, requestedQuantities),
                ProductQuantityEventStatus.READY,
                0,
                null,
                CREATED_AT,
                null
        );
    }
}
