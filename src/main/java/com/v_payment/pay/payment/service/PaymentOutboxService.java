package com.v_payment.pay.payment.service;

import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.exception.PaymentNotFoundException;
import com.v_payment.pay.payment.metric.PaymentOutboxMetric;
import com.v_payment.pay.payment.exception.PaymentOutboxNotFoundException;
import com.v_payment.pay.payment.repository.PaymentOutboxRepository;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.repository.PaymentRepository;
import com.v_payment.pay.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.infra.Result;
import com.v_payment.pay.payment.infra.SuccessResult;
import com.v_payment.pay.payment.infra.TossPayment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j(topic = "SCHEDULER_LOGGER")
@Service
@RequiredArgsConstructor
public class PaymentOutboxService {
    private final Clock clock;
    private final TossPayment tossPayment;
    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentLedgerService paymentLedgerService;
    private final PaymentOutboxMetric paymentOutboxMetric;

    public Result approve(PaymentPayload paymentPayload) {
        return tossPayment.call(paymentPayload);
    }

    @Transactional
    public boolean claimReady(Long id) {
        return paymentOutboxRepository.markProcessing(id, LocalDateTime.now(clock)) == 1;
    }

    @Transactional
    public void postApprove(Result result, Long id, PaymentPayload paymentPayload) {
        if (result instanceof SuccessResult successResult) {
            applySuccessResult(successResult, id, paymentPayload);
        }
        if (result instanceof FailedResult failedResult) {
            applyFailedResult(failedResult, id, paymentPayload);
        }
    }

    private void applySuccessResult(SuccessResult successRes, Long id, PaymentPayload paymentPayload) {
        int updated = paymentOutboxRepository.markPublished(id, LocalDateTime.now(clock));
        if (updated == 0) {
            throw new PaymentOutboxNotFoundException("PROCESSING outbox not found.");
        }

        updated = paymentRepository.markApproved(paymentPayload.getOrderId(), PaymentStatus.APPROVING,
                PaymentStatus.APPROVED, successRes.totalAmount(), successRes.approvedAt(), successRes.receipt().url());
        if (updated == 0) {
            throw new PaymentNotFoundException("APPROVING payment not found.");
        }

        paymentLedgerService.insertPaymentLedgerAPPROVED(paymentPayload, successRes);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                paymentOutboxMetric.incrementCompleted(1);
            }
        });
    }

    private void applyFailedResult(FailedResult failedRes, Long id, PaymentPayload paymentPayload) {
        int updated = paymentOutboxRepository.markPublished(id, LocalDateTime.now(clock));
        if (updated == 0) {
            throw new PaymentOutboxNotFoundException("PROCESSING outbox not found.");
        }

        updated = paymentRepository.markRejected(paymentPayload.getOrderId(), PaymentStatus.APPROVING,
                PaymentStatus.REJECTED, failedRes.message());
        if (updated == 0) {
            throw new PaymentNotFoundException("APPROVING payment not found.");
        }

        paymentLedgerService.insertPaymentLedgerREJECTED(paymentPayload, failedRes);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                paymentOutboxMetric.incrementCompleted(1);
            }
        });
    }
}
