package com.v_payment.pay.payment.service;

import com.v_payment.pay.payment.domain.PaymentRecoveryPolicy;
import com.v_payment.pay.payment.domain.entity.Payment;
import com.v_payment.pay.payment.domain.entity.PaymentPayload;
import com.v_payment.pay.payment.infra.result.Result;
import com.v_payment.pay.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j(topic = "SCHEDULER_LOGGER")
@Service
@RequiredArgsConstructor
public class PaymentRecoveryService {
    private final Clock clock;
    private final PaymentRepository paymentRepository;
    private final PaymentRecoveryPolicy paymentRecoveryPolicy;
    private final PaymentApprovalService paymentApprovalService;

    @Transactional
    public List<PaymentPayload> findRecoveryTargets() {
        LocalDateTime staleTime = paymentRecoveryPolicy.getStaleTime(LocalDateTime.now(clock));

        List<Payment> recoverablePayments = paymentRepository.findRecoverablePayments(
                paymentRecoveryPolicy.getRecoverableStatuses(), staleTime, paymentRecoveryPolicy.pageRequest());

        return recoverablePayments.stream()
                .map(this::increaseAttemptCountAndCreatePayload)
                .toList();
    }

    private PaymentPayload increaseAttemptCountAndCreatePayload(Payment payment) {
        payment.increaseRecoveryAttemptCount();

        return PaymentPayload.create(
                payment.getOrderCode(),
                payment.getIdempotencyKey(),
                payment.getPaymentKey(),
                payment.getRequestedAmount()
        );
    }

    public void finalizePaymentPayloads(List<Result> approveResults) {
        for(Result result : approveResults) {
            try{
                paymentApprovalService.finalizePaymentPayload(result);
            } catch (Exception e) {
                log.error("결제 복구를 실패했습니다. idempotencyKey={}, error={}",
                        result.getIdempotencyKey(), e.toString());
            }
        }
    }
}
