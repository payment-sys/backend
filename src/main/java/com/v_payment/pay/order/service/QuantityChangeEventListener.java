package com.v_payment.pay.order.service;

import com.v_payment.pay.order.domain.outbox.QuantityChangeOutbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuantityChangeEventListener {
    private final QuantityChangesEventService quantityChangesEventService;
    private final ExecutorService quantityChangeEventExecutorService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(QuantityChangeOutbox outbox) {
        CompletableFuture.runAsync(() -> {
                    quantityChangesEventService.publish(outbox);
                    quantityChangesEventService.markDone(outbox.getId());
                }, quantityChangeEventExecutorService)
                .exceptionally(ex -> {
                    log.error("message 발행 서비스에서 문제가 발생하였습니다.  outboxId={}, orderCode={}",
                            outbox.getId(), outbox.getOrderCode(), ex);
                    //todo: 계속 실패하는 이벤트 DLQ 처리 추가예정
                    return null;
                });
    }
}
