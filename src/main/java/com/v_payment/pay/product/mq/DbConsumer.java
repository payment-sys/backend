package com.v_payment.pay.product.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DbConsumer implements QueueConsumer {
    private static final int MAX_BATCH_SIZE = 200;

    private final ConsumeHandler consumeHandler;

    @Override
    @Scheduled(fixedDelay = 200)
    public void consume() {
        consumeHandler.handleReadyEvents(MAX_BATCH_SIZE);
    }

    @Scheduled(fixedDelay = 1000)
    public void consumeRetry() {
        consumeHandler.handleRetryEvents(MAX_BATCH_SIZE);
    }
}
