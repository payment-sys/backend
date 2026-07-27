package com.v_payment.pay.payment.service;

import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.infra.toss.TossPayment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j(topic = "SCHEDULER_LOGGER")
@Component
@RequiredArgsConstructor
public class PaymentRecoveryScheduler {
    private final TossPayment tossPayment;
    private final PaymentService paymentService;

    @Scheduled(fixedDelayString = "${payment.recovery.fixed-delay-ms:30000}")
    public void recoverPayments() {
        List<PaymentRecoveryTarget> recoveryTargets = paymentService.findRecoveryTargets();
        for (PaymentRecoveryTarget recoveryTarget : recoveryTargets) {
            PaymentPayload paymentPayload = recoveryTarget.paymentPayload();
            try {
                int currentAttemptCount = recoveryTarget.recoveryAttemptCount() + 1;
                if (currentAttemptCount >= 2) {
                    log.error("payment recovery retried more than once. orderCode = {}, attemptCount = {}",
                            paymentPayload.getOrderCode(), currentAttemptCount);
                }
                paymentService.increaseRecoveryAttemptCount(paymentPayload.getOrderCode());
                paymentService.finalizePaymentPayload(tossPayment.approve(paymentPayload));
            } catch (RuntimeException e) {
                log.error("payment recovery failed. orderCode = {}, error = {}",
                        paymentPayload.getOrderCode(), e.toString());
            }
        }
        log.info("payment recovery scheduler completed.");
    }
}
