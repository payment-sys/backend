package com.v_payment.pay.product.service;

import com.v_payment.pay.product.config.ProductQuantityEventConsumerProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProductQuantityEventSchedulerTest {

    @DisplayName("스케줄러는 설정된 batchSize로 READY 이벤트 소비를 요청한다")
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

    @DisplayName("스케줄러는 설정된 batchSize로 RETRY 이벤트 소비를 요청한다")
    @Test
    void consumeRetry() {
        // given
        ProductQuantityEventFacade facade = mock(ProductQuantityEventFacade.class);
        ProductQuantityEventScheduler scheduler = new ProductQuantityEventScheduler(
                facade,
                new ProductQuantityEventConsumerProperties(30)
        );

        // when
        scheduler.consumeRetry();

        // then
        verify(facade).consumeRetryEvent(30);
    }
}
