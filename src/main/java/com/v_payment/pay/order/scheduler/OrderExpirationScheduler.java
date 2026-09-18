package com.v_payment.pay.order.scheduler;

import com.v_payment.pay.order.service.OrderExpirationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j(topic = "SCHEDULER_LOGGER")
@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {
    private final OrderExpirationService orderExpirationService;

    @Scheduled(fixedDelayString = "${order.expiration.fixed-delay-ms:30000}")
    @SchedulerLock(
            name = "orderExpiration.expireCreatedOrders",
            lockAtMostFor = "10s",
            lockAtLeastFor = "1s"
    )
    public void expireCreatedOrders() {
        int expiredCount = orderExpirationService.expireCreatedOrders();
        log.info("order 만료 스케쥴러가 완료되었습니다. expiredCount={}", expiredCount);
    }
}
