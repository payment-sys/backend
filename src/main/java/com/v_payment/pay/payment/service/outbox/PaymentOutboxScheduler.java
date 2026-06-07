package com.v_payment.pay.payment.service.outbox;

import com.v_payment.pay.payment.entity.outbox.PaymentPayload;
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
public class PaymentOutboxScheduler {
    private static final int MIN_BATCH_SIZE = 20;
    private static final int MAX_BATCH_SIZE = 100;

    private final PaymentOutboxLimiter paymentOutboxLimiter;
    private final PaymentOutboxService paymentOutboxService;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @Scheduled(fixedDelay = 500)
    public void schedulePaymentOutbox() {
        int batchableSize = Math.min(paymentOutboxLimiter.getAvailableCount(), MAX_BATCH_SIZE);
        if (batchableSize < MIN_BATCH_SIZE) return;
        paymentOutboxLimiter.acquire(batchableSize);

        List<Long> ids = paymentOutboxService.loadApproves(batchableSize);
        int unusedCount = batchableSize - ids.size();

        if (unusedCount > 0) paymentOutboxLimiter.release(unusedCount);

        for (Long id : ids) {
            executorService.submit(() -> {
                try{
                    approvePipeline(id);
                } finally {
                    paymentOutboxLimiter.release();
                }
            });
        }
    }

    private void approvePipeline(Long id) {
        try{
            PaymentPayload paymentPayload = paymentOutboxService.preApprove(id);

            Result result = paymentOutboxService.approve(paymentPayload);

            paymentOutboxService.postApprove(result, id);
        } catch (Exception e){
            log.error(e.getMessage());
        }
    }
}
