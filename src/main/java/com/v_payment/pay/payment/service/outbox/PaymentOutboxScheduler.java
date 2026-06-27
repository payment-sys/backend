package com.v_payment.pay.payment.service.outbox;

import com.v_payment.pay.payment.entity.outbox.PaymentPayload;
import com.v_payment.pay.payment.infra.Result;
import com.v_payment.pay.payment.service.limiter.Limiter;
import com.v_payment.pay.payment.service.outbox.exception.PaymentNotFoundException;
import com.v_payment.pay.payment.service.outbox.exception.PaymentOutboxNotFoundException;
import com.v_payment.pay.payment.service.outbox.queue.OutboxQueue;
import com.v_payment.pay.payment.service.outbox.queue.PaymentOutboxQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

@Slf4j(topic = "SCHEDULER_LOGGER")
@Component
public class PaymentOutboxScheduler implements SchedulingConfigurer {
    private final PaymentOutboxQueue outboxQueue;
    private final SchedulerManager schedulerManager;
    private final ThreadPoolTaskScheduler outboxTaskScheduler;
    private final Limiter resultApplyLimiter;
    private final Limiter virtualThreadLimiter;
    private final PaymentOutboxService paymentOutboxService;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    public PaymentOutboxScheduler(
            PaymentOutboxQueue outboxQueue,
            SchedulerManager schedulerManager,
            ThreadPoolTaskScheduler outboxTaskScheduler,
            @Qualifier("resultApplyLimiter") Limiter resultApplyLimiter,
            @Qualifier("virtualThreadLimiter") Limiter virtualThreadLimiter,
            PaymentOutboxService paymentOutboxService
    ) {
        this.outboxQueue = outboxQueue;
        this.schedulerManager = schedulerManager;
        this.outboxTaskScheduler = outboxTaskScheduler;
        this.resultApplyLimiter = resultApplyLimiter;
        this.virtualThreadLimiter = virtualThreadLimiter;
        this.paymentOutboxService = paymentOutboxService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(outboxTaskScheduler);
        taskRegistrar.addTriggerTask(this::schedulePaymentOutbox, schedulerManager::nextExecution);
    }

    public void schedulePaymentOutbox() {
        long startNanos = System.nanoTime();

        int batchableSize = schedulerManager.calculateBatchableSize(virtualThreadLimiter.getAvailableCount());
        if(batchableSize <= 0) return;

        List<PaymentOutboxTask> tasks = outboxQueue.poll(batchableSize);
        schedulerManager.applySchedulingDelay(tasks.size());

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
        schedulerManager.resetSchedulingDelay();
    }

    private void submitTaskToVirtualThread(PaymentOutboxTask task, long startNanos) {
        try {
            virtualThreadLimiter.executeWithoutRelease(1, () -> executorService.submit(() -> {
                try {
                    approvePipeline(task, startNanos);
                } finally {
                    virtualThreadLimiter.release();
                }
            }));
        } catch (RejectedExecutionException e) {
            virtualThreadLimiter.release();
            log.warn("가상 스레드 작업 제출에 실패했습니다. id = {} error = {}", task.id(), e.toString());
        }
    }

    private void approvePipeline(PaymentOutboxTask task, long startNanos) {
        try {
            PaymentPayload paymentPayload = task.paymentPayload();

            Result result = paymentOutboxService.approve(paymentPayload);

            resultApplyLimiter.execute(() -> paymentOutboxService.postApprove(result, task.id(), paymentPayload));
        } catch (DataAccessException e) {
            log.warn("approvePipeline을 수행할 수 없습니다. id = {} error = {}", task.id(), e.toString());
        } catch (PaymentNotFoundException | PaymentOutboxNotFoundException e) {
            log.warn("{} id = {}", e.getMessage(), task.id());
        } catch (RuntimeException e) {
            log.error("알 수 없는 에러 발생 error = {}", e.toString());
        } finally {
            PaymentOutboxMetric.recordTaskElapsed(System.nanoTime() - startNanos);
        }
    }
}
