package com.v_payment.pay.payment.service;

import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.controller.dto.req.PaymentCreateReq;
import com.v_payment.pay.payment.controller.dto.res.PaymentCreateRes;
import com.v_payment.pay.payment.entity.PaymentOutbox;
import com.v_payment.pay.payment.metric.PaymentOutboxMetric;
import com.v_payment.pay.payment.repository.PaymentOutboxRepository;
import com.v_payment.pay.payment.repository.PaymentRepository;
import com.v_payment.pay.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.entity.Provider;
import static com.v_payment.pay.payment.exception.PaymentException.*;

import com.v_payment.pay.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;


@Slf4j(topic = "API_LOGGER")
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final Clock clock;
    private final PaymentRepository paymentRepository;
    private final PaymentLedgerService paymentLedgerService;
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentOutboxMetric paymentOutboxMetric;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PaymentCreateRes create(PaymentCreateReq paymentCreateReq) {
        Payment newPayment = Payment.create(paymentCreateReq, clock);
        Payment savedPayment = paymentRepository.save(newPayment);

        paymentLedgerService.insertPaymentLedgerPENDING(savedPayment);
        return PaymentCreateRes.from(savedPayment);
    }

    @Transactional
    public void validateApprovalReq(ApprovalReq approvalReq) {
        //Payment 검증 및 상태 업뎃
        int updatedCount = paymentRepository.markApproving(approvalReq.orderId(), approvalReq.paymentKey(),
                approvalReq.requestedAmount(), approvalReq.provider(), approvalReq.method(), PaymentStatus.PENDING,
                PaymentStatus.APPROVING);
        if (updatedCount != 1) throw new BusinessException(PAYMENT_INVALID);

        //Payment 원장 테이블 저장
        paymentLedgerService.insertPaymentLedgerAPPROVING(approvalReq);

        //Payment 아웃박스 테이블 저장
        PaymentOutbox savedOutbox = paymentOutboxRepository.save(
                PaymentOutbox.create(approvalReq, LocalDateTime.now(clock)));

        eventPublisher.publishEvent(new PaymentOutboxTask(savedOutbox.getId(), savedOutbox.getPaymentPayload()));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                paymentOutboxMetric.incrementEnqueued();
            }
        });
    }
}
