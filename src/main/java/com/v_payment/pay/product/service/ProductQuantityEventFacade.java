package com.v_payment.pay.product.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQuantityEventFacade {
    private final ProductQuantityEventService productQuantityEventService;
    private final MeterRegistry meterRegistry;
    private final AtomicInteger success = new AtomicInteger();
    private final AtomicInteger failure = new AtomicInteger();

    @PostConstruct
    void registerMetrics() {
        Gauge.builder("pay.scheduler.success", success, AtomicInteger::get)
                .description("Scheduler last execution success flag")
                .tag("scheduler", "product_quantity_event")
                .register(meterRegistry);
        Gauge.builder("pay.scheduler.failure", failure, AtomicInteger::get)
                .description("Scheduler last execution failure flag")
                .tag("scheduler", "product_quantity_event")
                .register(meterRegistry);
    }

    public void consumeReadyEvent(int batchSize) {
        try {
            productQuantityEventService.consumeReadyEvent(batchSize);
            success.set(1); failure.set(0);
        } catch (Exception e) {
            success.set(0); failure.set(1);
            log.error("상품 재고 차감 이벤트 소비 중 알 수 없는 예외 발생", e);
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("상품 재고 차감 이벤트 소비 중 알 수 없는 예외 발생", e);
        }
    }
}
