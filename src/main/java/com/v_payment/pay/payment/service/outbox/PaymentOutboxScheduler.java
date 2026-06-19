package com.v_payment.pay.payment.service.outbox;

import com.v_payment.pay.payment.entity.outbox.PaymentPayload;
import com.v_payment.pay.payment.infra.Result;
import com.v_payment.pay.payment.service.outbox.exception.PaymentNotFoundException;
import com.v_payment.pay.payment.service.outbox.exception.PaymentOutboxNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxScheduler {
    private static final int MIN_BATCH_SIZE = 10;
    private static final int MAX_BATCH_SIZE = 15;
    private final PaymentOutboxLimiter paymentOutboxLimiter;
    private final PaymentOutboxService paymentOutboxService;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @Scheduled(fixedDelay = 25)
    public void schedulePaymentOutbox() {
        int batchableSize = Math.min(paymentOutboxLimiter.getAvailableCount(), MAX_BATCH_SIZE);

        if (batchableSize < MIN_BATCH_SIZE) return;

        List<Long> ids = pollPaymentOutboxIds(batchableSize);

        ids.forEach(this::submitTaskToVirtualThread);
    }

    private List<Long> pollPaymentOutboxIds(int batchableSize) {
        try{
            List<Long> ids = paymentOutboxService.loadApproves(batchableSize);
            paymentOutboxLimiter.acquire(ids.size());
            return ids;
        } catch (DataAccessException e) {
            log.warn("PaymentOutbox를 가져오는데 실패했습니다.", e);
            return List.of();    //예외를 무시하고 빈 List를 던짐으로 빠르게 마무리
        }
    }

    private void submitTaskToVirtualThread(Long id) {
        try{
            executorService.submit(() -> approvePipeline(id));
        } catch (RejectedExecutionException e) {
            log.warn("가상 쓰레드 작업 제출 실패하였습니다. id = {}", id);
        }
    }

    private void approvePipeline(Long id) {
        try{
            PaymentPayload paymentPayload = paymentOutboxService.preApprove(id);

            Result result = paymentOutboxService.approve(paymentPayload);

            paymentOutboxService.postApprove(result, id);
        } catch (DataAccessException e){
            log.warn("approvePipeLine을 수행할 수 없습니다. id = {}", id, e);
        } catch (PaymentNotFoundException | PaymentOutboxNotFoundException e) {
            log.warn("{}id = {}", e.getMessage(), id, e);
        } catch (RuntimeException e) {
            log.error("알 수 없는 에러 발생", e);
        } finally {
            paymentOutboxLimiter.release();
        }
    }
}
