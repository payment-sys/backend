package com.v_payment.pay.product.mq;

import com.v_payment.pay.product.entity.ProductQuantityEventPayload;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class MqConsumer implements QueueConsumer {
    private static final int MAX_BATCH_SIZE = 100;

    private final ConsumeHandler consumeHandler;
    private final ProductQuantityMessageQueue mq;
    private final MqLog mqLog;
    private final ExecutorService mqExecutor;

    public MqConsumer(ConsumeHandler consumeHandler, ProductQuantityMessageQueue mq, MqLog mqLog) {
        this.consumeHandler = consumeHandler;
        this.mq = mq;
        this.mqLog = mqLog;
        this.mqExecutor = Executors.newSingleThreadExecutor(Thread.ofPlatform().factory());
    }

    @PostConstruct
    public void start() {
        mqExecutor.submit(this::process);
    }

    public void process() {
        while (!Thread.currentThread().isInterrupted()) {
            consume();
        }
    }

    @Override
    public void consume() {
        try {
            List<ProductQuantityEventPayload> payloads = mq.consumePayloads(MAX_BATCH_SIZE);
            handle(payloads);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("consumer thread exception occurred.", e);
        }
    }

    private void handle(List<ProductQuantityEventPayload> payloads) {
        mqLog.append("CONSUME", payloads);
        consumeHandler.handle(payloads);
    }

    @PreDestroy
    public void shutdown() {
        mqExecutor.shutdownNow();
    }
}
