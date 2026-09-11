package com.v_payment.pay.product.service;

import com.v_payment.pay.product.config.ProductQuantityEventConsumerProperties;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductQuantityEventScheduler {
    private final ProductQuantityEventFacade productQuantityEventFacade;
    private final ProductQuantityEventConsumerProperties properties;

    @Scheduled(fixedDelay = 200)
    @SchedulerLock(
            name = "productQuantityEvent.consumeReady",
            lockAtMostFor = "10s",
            lockAtLeastFor = "200ms"
    )
    public void consume() {
        productQuantityEventFacade.consumeReadyEvent(properties.batchSize());
    }

    @Scheduled(fixedDelay = 1000)
    @SchedulerLock(
            name = "productQuantityEvent.consumeRetry",
            lockAtMostFor = "10s",
            lockAtLeastFor = "1s"
    )
    public void consumeRetry() {
        productQuantityEventFacade.consumeRetryEvent(properties.batchSize());
    }
}
