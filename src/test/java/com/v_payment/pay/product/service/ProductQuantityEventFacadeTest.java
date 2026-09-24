package com.v_payment.pay.product.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProductQuantityEventFacadeTest {
    @DisplayName("Ready event consume exceptions are logged and propagated by facade")
    @Test
    void consumeReadyEventWithUnknownException() {
        // given
        ProductQuantityEventService productQuantityEventService = mock(ProductQuantityEventService.class);
        ProductQuantityEventFacade productQuantityEventFacade = new ProductQuantityEventFacade(
                productQuantityEventService,
                new SimpleMeterRegistry()
        );
        willThrow(new RuntimeException()).given(productQuantityEventService).consumeReadyEvent(10);

        // when
        assertThatThrownBy(() -> productQuantityEventFacade.consumeReadyEvent(10))
                .isInstanceOf(RuntimeException.class);

        // then
        verify(productQuantityEventService).consumeReadyEvent(10);
    }
}
