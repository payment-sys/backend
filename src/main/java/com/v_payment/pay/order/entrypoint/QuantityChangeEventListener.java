package com.v_payment.pay.order.entrypoint;

import com.v_payment.pay.order.domain.outbox.QuantityChangeOutbox;
import com.v_payment.pay.order.infrastructure.kafka.QuantityChangeProducer;
import com.v_payment.pay.order.infrastructure.kafka.dto.QuantityChangeMessage;
import com.v_payment.pay.order.application.QuantityChangeEventUseCase;
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
public class QuantityChangeEventListener {
    private final QuantityChangeProducer quantityChangeProducer;
    private final QuantityChangeEventUseCase quantityChangeEventUseCase;
    private final ExecutorService quantityChangeEventExecutorService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void listen(QuantityChangeOutbox outbox) {
        CompletableFuture
                .runAsync(() -> completeSendMessage(outbox), quantityChangeEventExecutorService)
                .whenComplete((unused, ex) -> {
                    if (ex != null) {
                        handleFailedSendMessage(outbox, ex);
                    }
                });
    }

    private void completeSendMessage(QuantityChangeOutbox outbox) {
        List<QuantityChangeMessage> quantityChangeMessages = outbox.getQuantityChangeMessages();
        List<CompletableFuture<?>> sendResults = new ArrayList<>();
        for(QuantityChangeMessage message : quantityChangeMessages) {
            sendResults.add(quantityChangeProducer.send(message));
        }
        CompletableFuture.allOf(sendResults.toArray(new CompletableFuture[0])).join();
        quantityChangeEventUseCase.markDone(outbox.getId());
    }

    private void handleFailedSendMessage(QuantityChangeOutbox outbox, Throwable ex) {
        log.error("message 발행 서비스에서 문제가 발생하였습니다.  outboxId={}, orderCode={}",
                outbox.getId(), outbox.getOrderCode(), ex);
        //todo: 계속 실패하는 이벤트 DLQ 처리 추가예정
    }

}
