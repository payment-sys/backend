package com.v_payment.pay.payment.service;

import com.v_payment.pay.global.BusinessException;
import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.req.PaymentCreateReq;
import com.v_payment.pay.payment.controller.dto.res.PaymentCreateRes;
import com.v_payment.pay.payment.entity.*;
import com.v_payment.pay.payment.entity.outbox.PaymentOutbox;
import com.v_payment.pay.payment.repository.PaymentOutboxRepository;
import com.v_payment.pay.payment.repository.PaymentRepository;
import com.v_payment.pay.payment.service.ledger.PaymentLedgerService;
import com.v_payment.pay.payment.service.outbox.PaymentOutboxMetric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;

import static com.v_payment.pay.payment.exception.PaymentException.*;

@Slf4j(topic = "API_LOGGER")
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final Clock clock;
    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentLedgerService paymentLedgerService;

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
        PaymentOutbox paymentOutbox = PaymentOutbox.create(approvalReq, LocalDateTime.now(clock));
        paymentOutboxRepository.save(paymentOutbox);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                PaymentOutboxMetric.incrementEnqueued();
            }
        });
    }
}
