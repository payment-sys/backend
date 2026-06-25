package com.v_payment.pay.payment.service.outbox.queue;

import com.v_payment.pay.payment.entity.outbox.PaymentOutbox;
import com.v_payment.pay.payment.entity.outbox.PaymentOutboxStatus;
import com.v_payment.pay.payment.entity.outbox.PaymentPayload;
import com.v_payment.pay.payment.repository.PaymentOutboxPublishProjection;
import com.v_payment.pay.payment.repository.PaymentOutboxRepository;
import com.v_payment.pay.payment.service.outbox.PaymentOutboxMetric;
import com.v_payment.pay.payment.service.outbox.PaymentOutboxTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j(topic = "SCHEDULER_LOGGER")
@Component
@RequiredArgsConstructor
public class PaymentOutboxQueue implements OutboxQueue{
    private final AtomicBoolean isEnqueued = new AtomicBoolean(false);

    private final Clock clock;
    private final ApplicationEventPublisher publisher;
    private final PaymentOutboxRepository paymentOutboxRepository;

    @Transactional
    @Override
    public void enqueue(PaymentOutbox paymentOutbox) {
        paymentOutboxRepository.save(paymentOutbox);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notifyEnqueued();
                recordMetric();
            }
        });
    }

    @Override
    public void notifyEnqueued() {
        if(isEnqueued.compareAndSet(false, true)) publisher.publishEvent(new OutboxEnqueueEvent());
    }

    @Override
    public void listenEnqueued() {
        isEnqueued.compareAndSet(true, false);
    }

    @Transactional
    @Override
    public List<PaymentOutboxTask> poll(int count) {
        try{
            List<PaymentOutboxPublishProjection> outboxes = paymentOutboxRepository.findForPublish(
                    PaymentOutboxStatus.READY.name(), LocalDateTime.now(clock), count);
            if(outboxes.isEmpty()) return List.of();

            List<Long> ids = outboxes.stream().map(PaymentOutboxPublishProjection::getPaymentOutboxId).toList();
            paymentOutboxRepository.markProcessing(ids);

            return outboxes.stream().map(outbox -> new PaymentOutboxTask(
                            outbox.getPaymentOutboxId(),
                            PaymentPayload.create(outbox.getOrderId(), outbox.getPaymentKey(), outbox.getAmount())))
                    .toList();
        } catch (DataAccessException e){
            log.warn("PaymentOutbox를 가져오는데 실패했습니다.", e);
            return List.of();
        }
    }

    public void recordMetric() {
        PaymentOutboxMetric.incrementEnqueued();
    }
}
