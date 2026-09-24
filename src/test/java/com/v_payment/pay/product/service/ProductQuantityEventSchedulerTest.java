package com.v_payment.pay.product.service;

import com.v_payment.pay.product.config.ProductQuantityEventConsumerProperties;
import com.v_payment.pay.product.scheduler.ProductQuantityEventScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProductQuantityEventSchedulerTest {

    @DisplayName("Scheduler requests READY event consume with configured batch size")
    @Test
    void consume() {
        // given
        ProductQuantityEventFacade facade = mock(ProductQuantityEventFacade.class);
        ProductQuantityEventScheduler scheduler = new ProductQuantityEventScheduler(
                facade,
                new ProductQuantityEventConsumerProperties(30)
        );

        // when
        scheduler.consume();

        // then
        verify(facade).consumeReadyEvent(30);
    }
}
