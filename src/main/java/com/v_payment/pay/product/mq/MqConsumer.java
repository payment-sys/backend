package com.v_payment.pay.product.mq;

import com.v_payment.pay.product.entity.ProductQuantityEventPayload;
import com.v_payment.pay.product.service.ProductService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Component
public class MqConsumer {
    private static final int MAX_BATCH_SIZE = 100;

    private final MqConsumeHandler mqConsumeHandler;
    private final ProductQuantityMessageQueue mq;
    private final ExecutorService mqExecutor;

    public MqConsumer(MqConsumeHandler mqConsumeHandler, ProductQuantityMessageQueue mq) {
        this.mqConsumeHandler = mqConsumeHandler;
        this.mq = mq;
        this.mqExecutor = Executors.newSingleThreadExecutor(Thread.ofPlatform().factory());
    }

    @PostConstruct
    public void start() {
        mqExecutor.submit(this::process);
    }

    public void process() {
        while(!Thread.currentThread().isInterrupted()) {
            try {
                List<ProductQuantityEventPayload> payloads = mq.consumePayloads(MAX_BATCH_SIZE);

                mqConsumeHandler.reserve(payloads);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        mqExecutor.shutdownNow();
    }
}
