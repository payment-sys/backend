package com.v_payment.pay.product.service;

import com.v_payment.pay.payment.entity.PaymentMethod;
import com.v_payment.pay.product.domain.entity.ProductQuantityEvent;
import com.v_payment.pay.product.domain.entity.ProductQuantityEventPayload;
import com.v_payment.pay.product.domain.entity.ProductQuantityEventStatus;
import com.v_payment.pay.product.exception.ProductQuantityEventConsumeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductQuantityEventFacadeTest {
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 10, 0, 0);

    @Mock
    ProductQuantityEventService productQuantityEventService;

    @InjectMocks
    ProductQuantityEventFacade productQuantityEventFacade;

    @DisplayName("READY 이벤트 소비에 실패하면 재시도 상태로 변경한다")
    @Test
    void consumeReadyEventWithConsumeException() {
        // given
        ProductQuantityEvent event = event("ORDER-001");
        ProductQuantityEventConsumeException exception =
                new ProductQuantityEventConsumeException(List.of(1L), List.of(event), new RuntimeException());
        willThrow(exception).given(productQuantityEventService).consumeReadyEvent(10);

        // when
        productQuantityEventFacade.consumeReadyEvent(10);

        // then
        verify(productQuantityEventService).consumeReadyEvent(10);
        verify(productQuantityEventService).markRetry(List.of(event), List.of(1L));
    }

    @DisplayName("RETRY 이벤트 소비에 실패하면 재시도 상태로 변경한다")
    @Test
    void consumeRetryEventWithConsumeException() {
        // given
        ProductQuantityEvent event = event("ORDER-001");
        ProductQuantityEventConsumeException exception =
                new ProductQuantityEventConsumeException(List.of(1L), List.of(event), new RuntimeException());
        willThrow(exception).given(productQuantityEventService).consumeRetryEvent(10);

        // when
        productQuantityEventFacade.consumeRetryEvent(10);

        // then
        verify(productQuantityEventService).consumeRetryEvent(10);
        verify(productQuantityEventService).markRetry(List.of(event), List.of(1L));
    }

    @DisplayName("알 수 없는 READY 이벤트 소비 예외는 재시도 상태로 변경하지 않는다")
    @Test
    void consumeReadyEventWithUnknownException() {
        // given
        willThrow(new RuntimeException()).given(productQuantityEventService).consumeReadyEvent(10);

        // when
        productQuantityEventFacade.consumeReadyEvent(10);

        // then
        verify(productQuantityEventService).consumeReadyEvent(10);
        verify(productQuantityEventService, never()).markRetry(any(), any());
    }

    private ProductQuantityEvent event(String orderCode) {
        return new ProductQuantityEvent(
                orderCode,
                ProductQuantityEventPayload.of(orderCode, PaymentMethod.CARD, Map.of(1L, 1)),
                ProductQuantityEventStatus.READY,
                0,
                null,
                CREATED_AT,
                null
        );
    }
}
