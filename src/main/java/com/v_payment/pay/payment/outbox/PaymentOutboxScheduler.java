package com.v_payment.pay.payment.outbox;

import com.v_payment.pay.payment.outbox.entity.PaymentPayload;
import com.v_payment.pay.payment.outbox.exception.PaymentNotFoundException;
import com.v_payment.pay.payment.outbox.exception.PaymentOutboxNotFoundException;
import com.v_payment.pay.payment.outbox.limiter.Limiter;
import com.v_payment.pay.payment.outbox.queue.OutboxQueue;
import com.v_payment.pay.payment.outbox.queue.PaymentOutboxQueue;
import com.v_payment.pay.payment.payment.infra.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class PaymentOutboxScheduler implements SchedulingConfigurer {
    private final PaymentOutboxQueue outboxQueue;
    private final SchedulerManager schedulerManager;
    private final ThreadPoolTaskScheduler outboxTaskScheduler;
    private final Limiter paymentOutboxResultApplyLimiter;
    private final Limiter paymentOutboxVirtualThreadLimiter;
    private final PaymentOutboxService paymentOutboxService;
    private final PaymentOutboxMetric paymentOutboxMetric;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(outboxTaskScheduler);
        taskRegistrar.addTriggerTask(this::schedulePaymentOutbox, schedulerManager::nextExecution);
    }

    public void schedulePaymentOutbox() {
        long startNanos = System.nanoTime();

        try {
            int batchableSize = schedulerManager.calculateBatchableSize(paymentOutboxVirtualThreadLimiter.getAvailableCount());
            if (batchableSize <= 0) {
                return;
            }

            List<PaymentOutboxTask> tasks = outboxQueue.poll(batchableSize);
            schedulerManager.applySchedulingDelay(tasks.size());

            tasks.forEach(task -> submitTaskToVirtualThread(task, startNanos));
        } finally {
            paymentOutboxMetric.recordSchedulerCycle(System.nanoTime() - startNanos);
        }
    }

    @EventListener
    public void listenEnqueueEvent(OutboxQueue.OutboxEnqueueEvent event) {
        outboxTaskScheduler.execute(() -> {
            try {
                schedulePaymentOutbox();
            } finally {
                outboxQueue.listenEnqueued();
            }
        });
        schedulerManager.resetSchedulingDelay();
    }

    private void submitTaskToVirtualThread(PaymentOutboxTask task, long startNanos) {
        try {
            paymentOutboxVirtualThreadLimiter.executeWithoutRelease(1, () -> executorService.submit(() -> {
                try {
                    approvePipeline(task, startNanos);
                } finally {
                    paymentOutboxVirtualThreadLimiter.release();
                }
            }));
        } catch (RejectedExecutionException e) {
            paymentOutboxVirtualThreadLimiter.release();
            log.warn("virtual-thread task submit failed. id = {} error = {}", task.id(), e.toString());
        }
    }

    private void approvePipeline(PaymentOutboxTask task, long startNanos) {
        try {
            PaymentPayload paymentPayload = task.paymentPayload();
            Result result = paymentOutboxService.approve(paymentPayload);

            paymentOutboxResultApplyLimiter.execute(() -> paymentOutboxService.postApprove(result, task.id(), paymentPayload));
        } catch (DataAccessException e) {
            log.warn("approvePipeline failed. id = {} error = {}", task.id(), e.toString());
        } catch (PaymentNotFoundException | PaymentOutboxNotFoundException e) {
            log.warn("{} id = {}", e.getMessage(), task.id());
        } catch (RuntimeException e) {
            log.error("unexpected error = {}", e.toString());
        } finally {
            paymentOutboxMetric.recordTaskElapsed(System.nanoTime() - startNanos);
        }
    }
}
