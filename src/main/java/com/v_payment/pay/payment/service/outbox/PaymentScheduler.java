package com.v_payment.pay.payment.service.outbox;

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
            executorService.submit(() -> approvePipeline(id));
        }
    }

    private void approvePipeline(Long id) {
        try{
            paymentOutboxService.updateOutboxProcessing(id);
            Result result = paymentOutboxService.approve(id);
            paymentOutboxService.finalizePayment(result, id);
        } catch (Exception e){
            log.error("알 수 없는 에러가 발생했습니다.", e);
        }
    }
}
