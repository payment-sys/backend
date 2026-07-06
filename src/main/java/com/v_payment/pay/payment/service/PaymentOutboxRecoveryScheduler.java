package com.v_payment.pay.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j(topic = "SCHEDULER_LOGGER")
@Component
@RequiredArgsConstructor
public class PaymentOutboxRecoveryScheduler {
    private final PaymentOutboxRecoveryService paymentOutboxRecoveryService;

    @Scheduled(fixedDelayString = "${payment.outbox.recovery.fixed-delay-ms}")
    public void recoverStaleReady() {
        int recoveredCount = paymentOutboxRecoveryService.recoverStaleReady();
        if (recoveredCount > 0) {
            log.info("count = {} 개의 payment 이벤트를 복구하였습니다.", recoveredCount);
        }
    }
}
