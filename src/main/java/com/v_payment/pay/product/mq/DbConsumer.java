package com.v_payment.pay.product.mq;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DbConsumer {
    private static final int MAX_BATCH_SIZE = 200;

    private final ConsumeHandler consumeHandler;

    @Scheduled(fixedDelay = 200)
    @SchedulerLock(
            name = "productQuantityEvent.consumeReady",
            lockAtMostFor = "10s",
            lockAtLeastFor = "200ms"
    )
    public void consume() {
        consumeHandler.handleReadyEvents(MAX_BATCH_SIZE);
    }

    @Scheduled(fixedDelay = 1000)
    @SchedulerLock(
            name = "productQuantityEvent.consumeRetry",
            lockAtMostFor = "10s",
            lockAtLeastFor = "1s"
    )
    public void consumeRetry() {
        consumeHandler.handleRetryEvents(MAX_BATCH_SIZE);
    }
}
