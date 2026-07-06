package com.v_payment.pay.payment.service;

import com.v_payment.pay.payment.config.PaymentOutboxRecoveryProperties;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.repository.PaymentOutboxRepository;
import com.v_payment.pay.payment.repository.PaymentOutboxTaskProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentOutboxRecoveryService {
    private final Clock clock;
    private final PaymentOutboxRecoveryProperties properties;
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public int recoverReady() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minusSeconds(properties.staleAfterSeconds());
        int batchSize = properties.batchSize();

        List<PaymentOutboxTaskProjection> retryTasks = paymentOutboxRepository.findRetryReady(now, batchSize);
        int recoveredCount = publish(retryTasks);

        int remainingBatchSize = batchSize - recoveredCount;
        if (remainingBatchSize > 0) {
            List<PaymentOutboxTaskProjection> staleTasks = paymentOutboxRepository.findStaleReady(
                    cutoff, remainingBatchSize);
            recoveredCount += publish(staleTasks);
        }

        return recoveredCount;
    }

    private int publish(List<PaymentOutboxTaskProjection> tasks) {
        for (PaymentOutboxTaskProjection task : tasks) {
            PaymentPayload paymentPayload = PaymentPayload.create(
                    task.getOrderId(), task.getPaymentKey(), task.getAmount());
            eventPublisher.publishEvent(new PaymentOutboxTask(task.getPaymentOutboxId(), paymentPayload));
        }
        return tasks.size();
    }
}
