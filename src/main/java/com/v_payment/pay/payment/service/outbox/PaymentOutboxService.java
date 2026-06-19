package com.v_payment.pay.payment.service.outbox;

import com.v_payment.pay.payment.entity.*;
import com.v_payment.pay.payment.entity.outbox.PaymentOutbox;
import com.v_payment.pay.payment.entity.outbox.PaymentOutboxStatus;
import com.v_payment.pay.payment.entity.outbox.PaymentPayload;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.PaymentError;
import com.v_payment.pay.payment.infra.Result;
import com.v_payment.pay.payment.infra.SuccessResult;
import com.v_payment.pay.payment.infra.TossPayment;
import com.v_payment.pay.payment.repository.PaymentOutboxRepository;
import com.v_payment.pay.payment.repository.PaymentOutboxPublishProjection;
import com.v_payment.pay.payment.repository.PaymentRepository;
import com.v_payment.pay.payment.service.ledger.PaymentLedgerService;
import com.v_payment.pay.payment.service.outbox.exception.PaymentNotFoundException;
import com.v_payment.pay.payment.service.outbox.exception.PaymentOutboxNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

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

    //1. Batch로 처리해야할 이벤트들을 찾아온다.
    @Transactional
    public List<PaymentOutboxTask> loadApproves(int count) {
        List<PaymentOutboxPublishProjection> outboxes = paymentOutboxRepository.findForPublish(
                PaymentOutboxStatus.READY.name(), LocalDateTime.now(clock), count);
        if(outboxes.isEmpty()) return List.of();

        List<Long> ids = outboxes.stream().map(PaymentOutboxPublishProjection::getPaymentOutboxId).toList();
        int cnt = paymentOutboxRepository.markProcessing(ids);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                PaymentOutboxMetric.incrementClaimed(cnt);
            }
        });

        return outboxes.stream()
                .map(outbox -> new PaymentOutboxTask(
                        outbox.getPaymentOutboxId(),
                        PaymentPayload.create(outbox.getOrderId(), outbox.getPaymentKey(), outbox.getAmount())
                ))
                .toList();
    }

    //3. 외부 API 호출
    public Result approve(PaymentPayload paymentPayload) {
        return tossPayment.call(paymentPayload);
    }

    //4. 외부 API 결과 처리
    @Transactional
    public void postApprove(Result result, Long id) {
        if(result instanceof SuccessResult successResult) {
            applySuccessResult(successResult, id);
        }
        if(result instanceof FailedResult failedResult) {
            applyFailedResult(failedResult, id);
        }
    }

    //4-1. 성공 처리
    private void applySuccessResult(SuccessResult successResult, Long id) {
        PaymentOutbox paymentOutbox = paymentOutboxRepository.findByIdAndStatus(id, PaymentOutboxStatus.PROCESSING)
                .orElseThrow(() -> new PaymentOutboxNotFoundException("PROCESSING 상태의 outbox를 찾을 수 없습니다."));
        paymentOutbox.success();

        Payment payment = paymentRepository.findByOrderIdAndPaymentStatus(paymentOutbox.getOrderId(), PaymentStatus.APPROVING)
                .orElseThrow(() -> new PaymentNotFoundException("APPROVING 상태의 Payment를 찾을 수 없습니다."));
        payment.success(successResult);

        paymentLedgerService.insertPaymentLedgerAPPROVED(payment, successResult);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                PaymentOutboxMetric.incrementCompleted(1);
            }
        });
    }

    //4-2. 실패 처리
    private void applyFailedResult(FailedResult failedResult, Long id) {
        PaymentOutbox paymentOutbox = paymentOutboxRepository.findByIdAndStatus(id,  PaymentOutboxStatus.PROCESSING)
                .orElseThrow(() -> new PaymentOutboxNotFoundException("Processing 상태의 outbox를 찾을 수 없습니다."));

        if (!isRetryable(failedResult) || paymentOutbox.getAttemptCount() >= MAX_ATTEMPT_COUNT) {
            applyDeadResult(failedResult, paymentOutbox); return;
        }

        paymentOutbox.failed(failedResult, LocalDateTime.now(clock).plusSeconds(RETRY_DELAY_SECONDS));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                PaymentOutboxMetric.incrementRetryScheduled(1);
            }
        });
    }

    //4-2-1. 실패 중 재시도 불가 시 Dead 처리
    private void applyDeadResult(FailedResult failedResult, PaymentOutbox paymentOutbox) {
        paymentOutbox.dead(failedResult);

        Payment payment = paymentRepository.findByOrderIdAndPaymentStatus(failedResult.orderId(), PaymentStatus.APPROVING)
                .orElseThrow(() -> new PaymentNotFoundException("APPROVING 상태의 Payment를 찾을 수 없습니다."));
        payment.failed(failedResult);

        paymentLedgerService.insertPaymentLedgerREJECTED(payment, failedResult);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                PaymentOutboxMetric.incrementDiscarded(1);
            }
        });
    }

    private boolean isRetryable(FailedResult failedResult) {
        return failedResult.paymentError() == PaymentError.NETWORK_TIMEOUT ||
                failedResult.paymentError() == PaymentError.UPSTREAM_429 ||
                failedResult.paymentError() == PaymentError.UPSTREAM_5XX;
    }
}
