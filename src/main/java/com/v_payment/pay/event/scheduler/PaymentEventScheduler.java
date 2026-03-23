package com.v_payment.pay.event.scheduler;

import com.v_payment.pay.event.entity.PaymentEvent;
import com.v_payment.pay.event.service.PaymentEventService;
import com.v_payment.pay.payment.entity.PaymentPayload;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventScheduler {
    private final ExecutorService paymentEventExecutor;
    private final PaymentEventService paymentEventService;

    @Scheduled(fixedDelay = 5000)
    public void approveEvent() {
        List<PaymentPayload> pendingPaymentPayload = paymentEventService.findPendingPaymentEvent(100);

        pendingPaymentPayload.forEach(this::approve);
    }

    private void approve(PaymentPayload paymentPayload) {
        CompletableFuture.supplyAsync(() -> paymentEventService.approve(paymentPayload), paymentEventExecutor)
            .thenAccept(paymentEventService::saveResult);

    }
}
