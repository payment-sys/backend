package com.v_payment.pay.order.service;

import com.v_payment.pay.order.domain.outbox.QuantityChangeOutbox;
import com.v_payment.pay.order.domain.outbox.QuantityChangeOutboxStatus;
import com.v_payment.pay.order.infra.kafka.QuantityChangesProducer;
import com.v_payment.pay.order.repository.QuantityChangeOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuantityChangesEventService {
    private final Clock clock;
    private final QuantityChangesProducer quantityChangesProducer;
    private final QuantityChangeOutboxRepository quantityChangeOutboxRepository;

    public void publish(QuantityChangeOutbox outbox) {
        try {
            publishInternal(outbox);
        } catch (Exception e) {
            log.error("quantity change outbox 발행이 실패하였습니다. outboxId={}, orderCode={}",
                    outbox.getId(), outbox.getOrderCode(), e);
            throw e;
        }
    }

    private void publishInternal(QuantityChangeOutbox outbox) {
        List<CompletableFuture<?>> sendResults = outbox.publishEach(quantityChangesProducer::send);

        CompletableFuture.allOf(sendResults.toArray(CompletableFuture[]::new)).join();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDone(Long outboxId) {
        int doneCount = quantityChangeOutboxRepository.markDone(
                outboxId,
                QuantityChangeOutboxStatus.READY,
                QuantityChangeOutboxStatus.DONE,
                LocalDateTime.now(clock)
        );

        if(doneCount != 1) log.warn("아웃박스 발행 성공 상태 변경 실패하였습니다. outboxId={}", outboxId);
    }
}
