package com.v_payment.pay.payment.service.ledger;

import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.SuccessResult;
import com.v_payment.pay.payment.repository.PaymentLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentLedgerService {
    private final Clock clock;
    private final PaymentLedgerRepository paymentLedgerRepository;

    public void insertPaymentLedgerPENDING(Payment payment) {
        insertPaymentLedger(payment, null, PaymentStatus.PENDING, null, null,
                null);
    }

    public void insertPaymentLedgerAPPROVING(Payment payment) {
        insertPaymentLedger(payment, PaymentStatus.PENDING, PaymentStatus.APPROVING, null, null,
                null);
    }

    public void insertPaymentLedgerAPPROVED(Payment payment, SuccessResult successResult) {
        insertPaymentLedger(payment, PaymentStatus.APPROVING, PaymentStatus.APPROVED, null, null,
                successResult.totalAmount());
    }

    public void insertPaymentLedgerREJECTED(Payment payment, FailedResult failedResult) {
        insertPaymentLedger(payment, PaymentStatus.APPROVING, PaymentStatus.REJECTED, failedResult.paymentError().name(),
                failedResult.message(), null);
    }

    private void insertPaymentLedger(Payment payment,
                                     PaymentStatus fromStatus,
                                     PaymentStatus toStatus,
                                     String failedCode,
                                     String failedMessage,
                                     Long approvedAmount) {
        paymentLedgerRepository.insertPaymentLedger(
                payment.getOrderId(),
                payment.getPaymentKey(),
                payment.getProvider().name(),
                fromStatus,
                toStatus,
                failedCode,
                failedMessage,
                payment.getRequestedAmount(),
                approvedAmount,
                LocalDateTime.now(clock)
        );
    }
}
