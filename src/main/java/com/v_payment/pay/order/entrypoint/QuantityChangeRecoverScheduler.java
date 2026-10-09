package com.v_payment.pay.order.entrypoint;

import com.v_payment.pay.order.application.QuantityChangeRecoverUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j(topic = "SCHEDULER_LOGGER")
@Component
@RequiredArgsConstructor
public class QuantityChangeRecoverScheduler {
    private final QuantityChangeRecoverUseCase quantityChangeRecoverUseCase;

    @Scheduled(fixedDelayString = "${order.quantity-change-outbox-scheduler.fixed-delay-ms:30000}")
    @SchedulerLock(
            name = "quantityChangeOutbox.publishReady",
            lockAtMostFor = "10s",
            lockAtLeastFor = "1s"
    )
    public void publishReady() {
        quantityChangeRecoverUseCase.publishReady();
    }
}
