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
    private final ProductQuantityEventRepository productQuantityEventRepository;

    @Override
    @Scheduled(fixedDelay = 200)
    public void consume() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<ProductQuantityEvent> events = productQuantityEventRepository
                .findByProductQuantityEventStatusOrderByIdAsc(
                        ProductQuantityEventStatus.READY,
                        PageRequest.of(0, MAX_BATCH_SIZE)
                );
        if (events.isEmpty()) return;

        consumeHandler.handleEvents(events, now);
    }

    @Scheduled(fixedDelay = 1000)
    public void consumeRetry() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<ProductQuantityEvent> events = productQuantityEventRepository
                .findRetryable(now, PageRequest.of(0, MAX_BATCH_SIZE));
        if (events.isEmpty()) return;

        consumeHandler.handleEvents(events, now);
    }
}
