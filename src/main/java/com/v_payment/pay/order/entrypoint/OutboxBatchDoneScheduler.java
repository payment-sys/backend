package com.v_payment.pay.order.entrypoint;

import com.v_payment.pay.order.application.QuantityChangeEventUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxBatchDoneScheduler {
    private static final int BATCH_SIZE = 100;

    private final QuantityChangeEventUseCase quantityChangeEventUseCase;
    private final BlockingQueue<Long> doneOutboxIds = new LinkedBlockingQueue<>(10_000);
    private final AtomicInteger queuedCount = new AtomicInteger();

    public void enqueue(Long outboxId) {
        if (outboxId == null) {
            return;
        }

        doneOutboxIds.add(outboxId);
        queuedCount.incrementAndGet();
    }

    @Scheduled(fixedDelayString = "100")
    public void flush() {
        if (queuedCount.get() == 0) {
            return;
        }

        List<Long> outboxIds = new ArrayList<>();
        doneOutboxIds.drainTo(outboxIds, BATCH_SIZE);
        if (outboxIds.isEmpty()) return;

        try {
            quantityChangeEventUseCase.markDoneBatch(outboxIds);
        } catch (Exception e) {
            requeue(outboxIds);
            log.error("outbox DONE batch update 실패. count={}", outboxIds.size(), e);
        }
    }

    private void requeue(List<Long> outboxIds) {
        outboxIds.forEach(doneOutboxIds::add);
        queuedCount.addAndGet(outboxIds.size());
    }
}
