package com.v_payment.pay.payment.domain;

import com.v_payment.pay.payment.config.PaymentRecoveryProperties;
import com.v_payment.pay.payment.domain.entity.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentRecoveryPolicy {
    private static final List<PaymentStatus> RECOVERABLE_STATUSES = List.of(
            PaymentStatus.IN_PROGRESS,
            PaymentStatus.UNKNOWN
    );

    private final PaymentRecoveryProperties paymentRecoveryProperties;

    public LocalDateTime getStaleTime(LocalDateTime now) {
        return now.minusSeconds(paymentRecoveryProperties.staleAfterSeconds());
    }

    public List<PaymentStatus> getRecoverableStatuses() {
        return RECOVERABLE_STATUSES;
    }

    public PageRequest pageRequest() {
        return PageRequest.of(0, paymentRecoveryProperties.batchSize());
    }

    public int nextAttemptCount(Integer recoveryAttemptCount) {
        return currentAttemptCount(recoveryAttemptCount) + 1;
    }

    private int currentAttemptCount(Integer recoveryAttemptCount) {
        return recoveryAttemptCount == null ? 0 : recoveryAttemptCount;
    }
}
