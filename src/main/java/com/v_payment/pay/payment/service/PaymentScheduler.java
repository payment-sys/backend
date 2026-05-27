package com.v_payment.pay.payment.service;

import com.v_payment.pay.payment.infra.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentScheduler {
    private final PaymentOutboxService paymentOutboxService;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @Scheduled(fixedRate = 500)
    public void schedulePaymentOutbox() {
        List<Long> ids = paymentOutboxService.findIds(200);

        for (Long id : ids) {
            executorService.submit(() -> {
                Result result = paymentOutboxService.approve(id);
                paymentOutboxService.finalizePaymentPayload(id, result);
            });
        }
    }
}
