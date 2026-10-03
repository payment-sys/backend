package com.v_payment.pay.product.service;

import com.v_payment.pay.product.domain.event.QuantityChangeResultOutboxesEvent;
import com.v_payment.pay.product.domain.entity.QuantityChangeResultOutbox;
import com.v_payment.pay.product.infra.kafka.QuantityChangeResultProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuantityChangeResultEventListener {
    private final QuantityChangeResultProducer quantityChangeResultProducer;
    private final QuantityChangeResultOutboxService quantityChangeResultOutboxService;
    private final ExecutorService quantityChangeResultEventExecutorService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void listen(QuantityChangeResultOutboxesEvent event) {
        List<QuantityChangeResultOutbox> outboxes = event.outboxes();
        CompletableFuture
                .runAsync(() -> completeSends(outboxes), quantityChangeResultEventExecutorService)
                .whenComplete((unused, ex) -> handleFail(outboxes, ex));
    }

    private void completeSends(List<QuantityChangeResultOutbox> outboxes) {
        List<CompletableFuture<?>> sendResults = new ArrayList<>();
        for (QuantityChangeResultOutbox outbox : outboxes) {
            sendResults.add(quantityChangeResultProducer.send(outbox.getQuantityChangeResult()));
        }
        sendResults.forEach(CompletableFuture::join);
        quantityChangeResultOutboxService.markDoneBatch(outboxes);
    }

    private void handleFail(List<QuantityChangeResultOutbox> outboxes, Throwable ex) {
        if (ex == null) return;
        log.error(ex.getMessage(), ex);
        //TODO: DLQ 추가
    }
}
