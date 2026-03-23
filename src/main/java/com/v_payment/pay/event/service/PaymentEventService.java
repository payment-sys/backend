package com.v_payment.pay.event.service;

import com.v_payment.pay.event.repository.PaymentEventRepository;
import com.v_payment.pay.event.entity.PaymentEvent;
import com.v_payment.pay.global.BusinessException;
import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.exception.PaymentException;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.PaymentError;
import com.v_payment.pay.payment.infra.Result;
import com.v_payment.pay.payment.infra.SuccessResult;
import com.v_payment.pay.payment.infra.TossPayment;
import com.v_payment.pay.payment.repository.PaymentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentEventService {
    private final TossPayment tossPayment;
    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;

    @Transactional
    public List<PaymentPayload> findPendingPaymentEvent(int count) {
        List<PaymentEvent> paymentEvents = paymentEventRepository.findAllByPendingStatusLimit(
            count);


        return paymentEvents.stream()
            .map(PaymentEvent::getPaymentPayload)
            .toList();
    }

    public Result approve(PaymentPayload paymentPayload){
        return tossPayment.approve(paymentPayload);
    }

    @Transactional
    public void saveResult(Result result) {
        if(result instanceof SuccessResult successResult) {
            applySuccessResult(successResult);
        } else if(result instanceof FailedResult failedResult) {
            applyFailedResult(failedResult);
        }
    }

    private void applySuccessResult(SuccessResult successResult) {
        PaymentEvent paymentEvent = paymentEventRepository.findByOrderId(successResult.orderId());

        paymentEvent.success();

        Payment payment = paymentRepository.findByOrderId(successResult.orderId(), PaymentStatus.APPROVING)
            .orElseThrow(() -> new BusinessException(PaymentException.PAYMENT_NOT_FOUND));

        payment.success(successResult);
    }

    private void applyFailedResult(FailedResult failedResult) {

    }
}
