package com.v_payment.pay.product.scheduler;

import com.v_payment.pay.product.config.QuantityChangeResultOutboxPublishProperties;
import com.v_payment.pay.product.domain.event.QuantityChangeResultOutboxesEvent;
import com.v_payment.pay.product.domain.entity.QuantityChangeResultOutbox;
import com.v_payment.pay.product.domain.entity.QuantityChangeResultOutboxStatus;
import com.v_payment.pay.product.repository.QuantityChangeResultOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j(topic = "SCHEDULER_LOGGER")
@Component
@RequiredArgsConstructor
public class QuantityChangeResultOutboxPublishScheduler {
    private final Clock clock;
    private final QuantityChangeResultOutboxRepository quantityChangeResultOutboxRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final QuantityChangeResultOutboxPublishProperties properties;

    @Scheduled(fixedDelayString = "${product.quantity-change-result-outbox-scheduler.fixed-delay-ms:30000}")
    @SchedulerLock(
            name = "quantityChangeResultOutbox.publishReady",
            lockAtMostFor = "10s",
            lockAtLeastFor = "1s"
    )
    @Transactional
    public void publishReady() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime publishBefore = now.minusSeconds(properties.retryDelaySeconds());
        List<QuantityChangeResultOutbox> outboxes = quantityChangeResultOutboxRepository.findReadyForPublish(
                QuantityChangeResultOutboxStatus.READY.name(),
                now,
                publishBefore,
                properties.batchSize()
        );

        if (!outboxes.isEmpty()) {
            eventPublisher.publishEvent(new QuantityChangeResultOutboxesEvent(outboxes));
        }

        log.info("quantity change result outbox publish scheduler completed. eventPublishedCount={}", outboxes.size());
    }
}
