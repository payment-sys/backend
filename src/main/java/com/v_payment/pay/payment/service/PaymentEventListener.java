package com.v_payment.pay.payment.service;

import com.v_payment.pay.payment.limiter.Limiter;
import com.v_payment.pay.payment.infra.Result;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {
    private final PaymentOutboxService paymentOutboxService;
    private final Limiter paymentOutboxResultApplyLimiter;

    @Async("paymentSubmitExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void call(PaymentOutboxTask paymentOutboxTask) {
        Result result = paymentOutboxService.approve(paymentOutboxTask.paymentPayload());

        paymentOutboxResultApplyLimiter.execute(() -> paymentOutboxService.postApprove(
                result, paymentOutboxTask.id(), paymentOutboxTask.paymentPayload()));
    }
}
