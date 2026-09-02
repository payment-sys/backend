package com.v_payment.pay.product.mq;

import com.v_payment.pay.product.entity.ProductQuantityEvent;
import com.v_payment.pay.product.entity.ProductQuantityEventStatus;
import com.v_payment.pay.product.repository.ProductQuantityEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DbConsumer implements QueueConsumer {
    private static final int MAX_BATCH_SIZE = 200;

    private final Clock clock;
    private final ConsumeHandler consumeHandler;
    private final ProductQuantityConsumerMeter consumerMeter;
    private final ProductQuantityEventRepository productQuantityEventRepository;

    @Override
    @Scheduled(fixedDelay = 200)
    public void consume() {
        consumerMeter.recordRun(this::consumeReady);
    }

    private void consumeReady() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<ProductQuantityEvent> events = consumerMeter.recordPhase(
                ProductQuantityConsumerMeter.SOURCE_READY,
                "poll",
                () -> productQuantityEventRepository.findByProductQuantityEventStatusOrderByIdAsc(
                        ProductQuantityEventStatus.READY.toString(),
                        MAX_BATCH_SIZE
                )
        );
        consumerMeter.recordFetchedBatch(ProductQuantityConsumerMeter.SOURCE_READY, events, now);
        if (events.isEmpty()) return;

        consumeHandler.handleEvents(events, now, ProductQuantityConsumerMeter.SOURCE_READY);
    }

    @Scheduled(fixedDelay = 1000)
    public void consumeRetry() {
        consumerMeter.recordRun(this::consumeRetryable);
    }

    private void consumeRetryable() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<ProductQuantityEvent> events = consumerMeter.recordPhase(
                ProductQuantityConsumerMeter.SOURCE_RETRY,
                "poll",
                () -> productQuantityEventRepository.findRetryable(now, PageRequest.of(0, MAX_BATCH_SIZE))
        );
        consumerMeter.recordFetchedBatch(ProductQuantityConsumerMeter.SOURCE_RETRY, events, now);
        if (events.isEmpty()) return;

        consumeHandler.handleEvents(events, now, ProductQuantityConsumerMeter.SOURCE_RETRY);
    }
}
