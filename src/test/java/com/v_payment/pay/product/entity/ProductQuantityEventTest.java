package com.v_payment.pay.product.entity;

import com.v_payment.pay.payment.entity.PaymentMethod;
import com.v_payment.pay.product.domain.entity.ProductQuantityEvent;
import com.v_payment.pay.product.domain.entity.ProductQuantityEventPayload;
import com.v_payment.pay.product.domain.entity.ProductQuantityEventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductQuantityEventTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-10T00:00:00Z"),
            ZoneId.of("UTC")
    );
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 10, 0, 0);

    @DisplayName("상품 수량 이벤트를 READY 상태로 생성한다")
    @Test
    void of() {
        // given
        String orderCode = "ORDER-001";
        ProductQuantityEventPayload payload = ProductQuantityEventPayload.of(
                orderCode,
                PaymentMethod.CARD,
                Map.of(1L, 2)
        );

        // when
        ProductQuantityEvent event = ProductQuantityEvent.of(orderCode, payload, FIXED_CLOCK);

        // then
        assertThat(event.getOrderCode()).isEqualTo(orderCode);
        assertThat(event.getPayload()).isSameAs(payload);
        assertThat(event.getProductQuantityEventStatus()).isEqualTo(ProductQuantityEventStatus.READY);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getNextAttemptTime()).isNull();
        assertThat(event.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(event.getUpdatedAt()).isNull();
    }

    @DisplayName("orderCode는 필수다")
    @Test
    void createWithNullOrderCode() {
        assertThatThrownBy(() -> createEvent(null, payload()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("orderCode는 필수입니다.");
    }

    @DisplayName("orderCode는 비어 있을 수 없다")
    @Test
    void createWithBlankOrderCode() {
        assertThatThrownBy(() -> createEvent(" ", payload()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("orderCode는 필수입니다.");
    }

    @DisplayName("payload는 필수다")
    @Test
    void createWithNullPayload() {
        assertThatThrownBy(() -> createEvent("ORDER-001", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("payload는 필수입니다.");
    }

    @DisplayName("productQuantityEventStatus는 필수다")
    @Test
    void createWithNullStatus() {
        assertThatThrownBy(() -> new ProductQuantityEvent(
                "ORDER-001",
                payload(),
                null,
                0,
                null,
                CREATED_AT,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("productQuantityEventStatus는 필수입니다.");
    }

    @DisplayName("retryCount는 필수다")
    @Test
    void createWithNullRetryCount() {
        assertThatThrownBy(() -> new ProductQuantityEvent(
                "ORDER-001",
                payload(),
                ProductQuantityEventStatus.READY,
                null,
                null,
                CREATED_AT,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("retryCount는 필수입니다.");
    }

    @DisplayName("retryCount는 음수일 수 없다")
    @Test
    void createWithNegativeRetryCount() {
        assertThatThrownBy(() -> new ProductQuantityEvent(
                "ORDER-001",
                payload(),
                ProductQuantityEventStatus.READY,
                -1,
                null,
                CREATED_AT,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("retryCount는 음수일 수 없습니다.");
    }

    @DisplayName("createdAt은 필수다")
    @Test
    void createWithNullCreatedAt() {
        assertThatThrownBy(() -> new ProductQuantityEvent(
                "ORDER-001",
                payload(),
                ProductQuantityEventStatus.READY,
                0,
                null,
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("createdAt은 필수입니다.");
    }

    private ProductQuantityEvent createEvent(String orderCode, ProductQuantityEventPayload payload) {
        return new ProductQuantityEvent(
                orderCode,
                payload,
                ProductQuantityEventStatus.READY,
                0,
                null,
                CREATED_AT,
                null
        );
    }

    private ProductQuantityEventPayload payload() {
        return ProductQuantityEventPayload.of(
                "ORDER-001",
                PaymentMethod.CARD,
                Map.of(1L, 2)
        );
    }
}
