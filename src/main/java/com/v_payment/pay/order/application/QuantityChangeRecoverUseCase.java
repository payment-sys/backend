package com.v_payment.pay.order.application;

import com.v_payment.pay.order.config.QuantityChangeOutboxPublishProperties;
import com.v_payment.pay.order.domain.outbox.QuantityChangeOutbox;
import com.v_payment.pay.order.domain.outbox.QuantityChangeOutboxStatus;
import com.v_payment.pay.order.infrastructure.persistence.repository.QuantityChangeOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuantityChangeRecoverUseCase {
    private final Clock clock;
    private final QuantityChangeOutboxRepository quantityChangeOutboxRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final QuantityChangeOutboxPublishProperties properties;

    @Transactional
    public void publishReady() {
        LocalDateTime publishBefore = LocalDateTime.now(clock)
                .minusSeconds(properties.retryDelaySeconds());
        List<QuantityChangeOutbox> outboxes = quantityChangeOutboxRepository.findReadyForPublish(
                QuantityChangeOutboxStatus.READY.name(),
                publishBefore,
                properties.batchSize()
        );

        outboxes.forEach(eventPublisher::publishEvent);

        log.info("quantity change outbox publish completed. eventPublishedCount={}", outboxes.size());
    }
}
