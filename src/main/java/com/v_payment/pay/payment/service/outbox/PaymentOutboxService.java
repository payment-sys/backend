package com.v_payment.pay.payment.service.outbox;

import com.v_payment.pay.payment.entity.*;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.PaymentError;
import com.v_payment.pay.payment.infra.Result;
import com.v_payment.pay.payment.infra.SuccessResult;
import com.v_payment.pay.payment.infra.TossPayment;
import com.v_payment.pay.payment.repository.PaymentOutboxRepository;
import com.v_payment.pay.payment.repository.PaymentRepository;
import com.v_payment.pay.payment.service.ledger.PaymentLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
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

    public List<Long> findIds(int count) {
        Pageable pageable = PageRequest.of(0, count);
        return paymentOutboxRepository.findForPublish(PaymentOutboxStatus.READY, LocalDateTime.now(clock), pageable);
    }

    @Transactional
    public void updateOutboxProcessing(Long id) {
        paymentOutboxRepository.updateOutboxProcessing(id);
    }

    public Result approve(Long id) {
        PaymentPayload paymentPayload = paymentOutboxRepository.findPaymentPayloadById(id)
                .orElseThrow(() -> new PaymentOutboxError(""));

        return tossPayment.call(paymentPayload);
    }

    @Transactional
    public void finalizePayment(Result result, Long id) {
        if(result instanceof SuccessResult successResult) {
            applySuccessResult(successResult, id);
        }
        if(result instanceof FailedResult failedResult) {
            applyFailedResult(failedResult, id);
        }
    }

    private void applySuccessResult(SuccessResult successResult, Long id) {
        PaymentOutbox paymentOutbox = paymentOutboxRepository.findById(id).get();
        paymentOutbox.updateStatus(PaymentOutboxStatus.PUBLISHED);

        Payment payment = paymentRepository.findById(id).get();
        payment.success(successResult);

        paymentLedgerService.insertPaymentLedgerAPPROVED(payment, successResult);
    }

    private void applyFailedResult(FailedResult failedResult, Long id) {
        PaymentOutbox paymentOutbox = paymentOutboxRepository.findById(id)
                .orElseThrow(() -> new PaymentOutboxError("PaymentOutbox를 찾을 수 없습니다. id=" + id));

        paymentOutbox.plusAttemptCount();
        paymentOutbox.updateLastErrorCode(failedResult.paymentError().name());
        paymentOutbox.updateLastErrorMessage(failedResult.message());

        if (isRetryable(failedResult) && paymentOutbox.getAttemptCount() < MAX_ATTEMPT_COUNT) {
            paymentOutbox.updateStatus(PaymentOutboxStatus.READY);
            paymentOutbox.updateNextAttemptTime(LocalDateTime.now(clock).plusSeconds(RETRY_DELAY_SECONDS));
            return;
        }

        applyDeadResult(failedResult, paymentOutbox);
    }

    private void applyDeadResult(FailedResult failedResult, PaymentOutbox paymentOutbox) {
        paymentOutbox.updateStatus(PaymentOutboxStatus.DEAD);
        paymentOutbox.updateNextAttemptTime(null);

        Payment payment = paymentRepository.findByOrderIdAndPaymentStatus(
                failedResult.orderId(),
                PaymentStatus.APPROVING
        ).orElseThrow(() -> new PaymentOutboxError("Payment를 찾을 수 없습니다. orderId=" + failedResult.orderId()));

        payment.failed(failedResult);
        paymentLedgerService.insertPaymentLedgerREJECTED(payment, failedResult);
    }

    private boolean isRetryable(FailedResult failedResult) {
        return failedResult.paymentError() == PaymentError.NETWORK_TIMEOUT ||
                failedResult.paymentError() == PaymentError.UPSTREAM_429 ||
                failedResult.paymentError() == PaymentError.UPSTREAM_5XX;
    }
}
