package com.v_payment.pay.payment.scheduler;

import com.v_payment.pay.payment.domain.entity.PaymentPayload;
import com.v_payment.pay.payment.infra.result.Result;
import com.v_payment.pay.payment.infra.toss.TossPayment;
import com.v_payment.pay.payment.service.PaymentRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j(topic = "SCHEDULER_LOGGER")
@Component
@RequiredArgsConstructor
public class PaymentRecoveryScheduler {
    private final PaymentRecoveryService paymentRecoveryService;
    private final TossPayment tossPayment;

    @Scheduled(fixedDelayString = "${payment.recovery.fixed-delay-ms:30000}")
    public void recoverPayments() {
        List<PaymentPayload> recoveryPayments = paymentRecoveryService.findRecoveryTargets();

        List<Result> results = new ArrayList<>();
        for(PaymentPayload paymentPayload : recoveryPayments) {
            try{
                Result approveResult = tossPayment.approve(paymentPayload);
                results.add(approveResult);
            } catch (Exception e){
                log.error("결제 복구 승인 요청을 실패했습니다. idempotencyKey={}, error={}",
                        paymentPayload.getIdempotencyKey(), e.toString());
            }
        }

        paymentRecoveryService.finalizePaymentPayloads(results);
    }
}
