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
    public int recoverStaleReady() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minusSeconds(properties.staleAfterSeconds());
        List<PaymentOutboxTaskProjection> staleTasks = paymentOutboxRepository.findStaleReady(
                cutoff, properties.batchSize());

        int recoveredCount = 0;
        for (PaymentOutboxTaskProjection staleTask : staleTasks) {
            PaymentPayload paymentPayload = PaymentPayload.create(
                    staleTask.getOrderId(), staleTask.getPaymentKey(), staleTask.getAmount());
            eventPublisher.publishEvent(new PaymentOutboxTask(staleTask.getPaymentOutboxId(), paymentPayload));
            recoveredCount++;
        }
        return recoveredCount;
    }
}
