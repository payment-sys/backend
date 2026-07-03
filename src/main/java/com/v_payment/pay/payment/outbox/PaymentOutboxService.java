package com.v_payment.pay.payment.outbox;

import com.v_payment.pay.payment.ledger.service.PaymentLedgerService;
import com.v_payment.pay.payment.outbox.entity.PaymentPayload;
import com.v_payment.pay.payment.outbox.exception.PaymentNotFoundException;
import com.v_payment.pay.payment.outbox.exception.PaymentOutboxNotFoundException;
import com.v_payment.pay.payment.outbox.repository.PaymentOutboxRepository;
import com.v_payment.pay.payment.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.payment.infra.FailedResult;
import com.v_payment.pay.payment.payment.infra.PaymentError;
import com.v_payment.pay.payment.payment.infra.Result;
import com.v_payment.pay.payment.payment.infra.SuccessResult;
import com.v_payment.pay.payment.payment.infra.TossPayment;
import com.v_payment.pay.payment.payment.repository.PaymentRepository;
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
    private static final int MAX_ATTEMPT_COUNT = 3;
    private static final long RETRY_DELAY_SECONDS = 1;

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
    public void postApprove(Result result, Long id, PaymentPayload paymentPayload) {
        if (result instanceof SuccessResult successResult) {
            applySuccessResult(successResult, id, paymentPayload);
        }
        if (result instanceof FailedResult failedResult) {
            applyFailedResult(failedResult, id, paymentPayload);
        }
    }

    private void applySuccessResult(SuccessResult successRes, Long id, PaymentPayload paymentPayload) {
        int updated = paymentOutboxRepository.markPublished(id);
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
        if (isRetryable(failedRes)) {
            int updated = paymentOutboxRepository.markReadyForRetry(id, failedRes.paymentError().name(),
                    failedRes.message(), LocalDateTime.now(clock).plusSeconds(RETRY_DELAY_SECONDS), MAX_ATTEMPT_COUNT);
            if (updated == 1) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        paymentOutboxMetric.incrementRetryScheduled(1);
                    }
                });
                return;
            }
        }

        applyDeadResult(failedRes, id, paymentPayload);
    }

    private void applyDeadResult(FailedResult failedRes, Long id, PaymentPayload paymentPayload) {
        int updated = paymentOutboxRepository.markDead(id, failedRes.paymentError().name(), failedRes.message());
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
                paymentOutboxMetric.incrementDiscarded(1);
            }
        });
    }

    private boolean isRetryable(FailedResult failedResult) {
        return failedResult.paymentError() == PaymentError.NETWORK_TIMEOUT
                || failedResult.paymentError() == PaymentError.UPSTREAM_429
                || failedResult.paymentError() == PaymentError.UPSTREAM_5XX;
    }
}
