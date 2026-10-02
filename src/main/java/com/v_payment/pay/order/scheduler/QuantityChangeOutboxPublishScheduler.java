package com.v_payment.pay.order.scheduler;

import com.v_payment.pay.order.config.QuantityChangeOutboxPublishProperties;
import com.v_payment.pay.order.domain.outbox.QuantityChangeOutbox;
import com.v_payment.pay.order.domain.outbox.QuantityChangeOutboxStatus;
import com.v_payment.pay.order.repository.QuantityChangeOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j(topic = "SCHEDULER_LOGGER")
@Component
@RequiredArgsConstructor
public class QuantityChangeOutboxPublishScheduler {
    private final QuantityChangeOutboxRepository quantityChangeOutboxRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final QuantityChangeOutboxPublishProperties properties;

    @Scheduled(fixedDelayString = "${order.quantity-changes.outbox-publish.fixed-delay-ms:30000}")
    @SchedulerLock(
            name = "quantityChangeOutbox.publishReady",
            lockAtMostFor = "10s",
            lockAtLeastFor = "1s"
    )
    @Transactional
    public void publishReady() {
        List<QuantityChangeOutbox> outboxes = quantityChangeOutboxRepository.findReadyForPublish(
                QuantityChangeOutboxStatus.READY.name(),
                properties.batchSize()
        );

        outboxes.forEach(eventPublisher::publishEvent);

        log.info("quantity change outbox publish scheduler completed. eventPublishedCount={}", outboxes.size());
    }
}
