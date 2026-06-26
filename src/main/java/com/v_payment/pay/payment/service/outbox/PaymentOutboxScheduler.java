package com.v_payment.pay.payment.service.outbox;

import com.v_payment.pay.payment.entity.outbox.PaymentPayload;
import com.v_payment.pay.payment.infra.Result;
import com.v_payment.pay.payment.service.outbox.exception.PaymentNotFoundException;
import com.v_payment.pay.payment.service.outbox.exception.PaymentOutboxNotFoundException;
import com.v_payment.pay.payment.service.outbox.queue.OutboxQueue;
import com.v_payment.pay.payment.service.outbox.queue.PaymentOutboxQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

@Slf4j(topic = "SCHEDULER_LOGGER")
@Component
@RequiredArgsConstructor
public class PaymentOutboxScheduler implements SchedulingConfigurer {
    private static final int MIN_BATCH_SIZE = 25;
    private static final int MAX_BATCH_SIZE = 50;
    private static final long MIN_POLLING_DELAY_MS = 1;
    private static final long MAX_POLLING_DELAY_MS = 5000;
    private static final int POLLING_DELAY_MULTIPLIER = 2;

    private final PaymentOutboxQueue outboxQueue;
    private final ThreadPoolTaskScheduler outboxTaskScheduler;
    private final PaymentOutboxLimiter paymentOutboxLimiter;
    private final PaymentOutboxService paymentOutboxService;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    private volatile long currentPollingDelayMs = MIN_POLLING_DELAY_MS;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(outboxTaskScheduler);
        taskRegistrar.addTriggerTask(this::schedulePaymentOutbox, this::nextExecution);
    }

    public void schedulePaymentOutbox() {
        long startNanos = System.nanoTime();

        int batchableSize = Math.min(paymentOutboxLimiter.getAvailableCount(), MAX_BATCH_SIZE);
        if (batchableSize < MIN_BATCH_SIZE) return;
        List<PaymentOutboxTask> tasks = outboxQueue.poll(batchableSize);
        paymentOutboxLimiter.acquire(tasks.size());

        if (tasks.isEmpty()) {
            increasePollingDelay();
            return;
        }
        resetPollingDelay();

        tasks.forEach(task -> submitTaskToVirtualThread(task, startNanos));
        PaymentOutboxMetric.recordSchedulerCycle(System.nanoTime() - startNanos);
    }

    @EventListener
    public void listenEnqueueEvent(OutboxQueue.OutboxEnqueueEvent event) {
        outboxTaskScheduler.execute(() -> {
            try{
                schedulePaymentOutbox();
            } finally {
                outboxQueue.listenEnqueued();
            }
        });
        resetPollingDelay();
    }

    private Instant nextExecution(TriggerContext triggerContext) {
        return Instant.now().plusMillis(currentPollingDelayMs);
    }

    private void increasePollingDelay() {
        currentPollingDelayMs = Math.min(MAX_POLLING_DELAY_MS, currentPollingDelayMs * POLLING_DELAY_MULTIPLIER);
    }

    private void resetPollingDelay() {
        currentPollingDelayMs = MIN_POLLING_DELAY_MS;
    }

    private void submitTaskToVirtualThread(PaymentOutboxTask task, long startNanos) {
        try {
            executorService.submit(() -> approvePipeline(task, startNanos));
        } catch (RejectedExecutionException e) {
            paymentOutboxLimiter.release();
            log.warn("가상 스레드 작업 제출에 실패했습니다. id = {}", task.id());
        }
    }

    private void approvePipeline(PaymentOutboxTask task, long startNanos) {
        try {

            PaymentPayload paymentPayload = task.paymentPayload();

            Result result = paymentOutboxService.approve(paymentPayload);

            paymentOutboxService.postApprove(result, task.id(), paymentPayload);
        } catch (DataAccessException e) {
            log.warn("approvePipeline을 수행할 수 없습니다. id = {}", task.id(), e);
        } catch (PaymentNotFoundException | PaymentOutboxNotFoundException e) {
            log.warn("{} id = {}", e.getMessage(), task.id(), e);
        } catch (RuntimeException e) {
            log.error("알 수 없는 에러 발생", e);
        } finally {
            PaymentOutboxMetric.recordTaskElapsed(System.nanoTime() - startNanos);
            paymentOutboxLimiter.release();
        }
    }
}
