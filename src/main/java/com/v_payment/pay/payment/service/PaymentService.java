package com.v_payment.pay.payment.service;

import com.v_payment.pay.global.BusinessException;
import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.req.PaymentCreateReq;
import com.v_payment.pay.payment.controller.dto.res.PaymentCreateRes;
import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.entity.PaymentStatus;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.Result;
import com.v_payment.pay.payment.infra.SuccessResult;
import com.v_payment.pay.payment.infra.TossPayment;
import com.v_payment.pay.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

import static com.v_payment.pay.payment.exception.PaymentException.*;

@Slf4j(topic = "API_LOGGER")
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final Clock clock;
    private final TossPayment tossPayment;
    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentCreateRes create(PaymentCreateReq paymentCreateReq) {
        Payment newPayment = Payment.create(paymentCreateReq, clock);
        Payment savedPayment = paymentRepository.save(newPayment);
        log.info("저장된 금액 = {}, 상태 = {}", savedPayment.getRequestedAmount(), savedPayment.getPaymentStatus());
        return PaymentCreateRes.from(savedPayment);
    }

    @Transactional
    public PaymentPayload validateApprovalReq(ApprovalReq approvalReq) {
        Payment payment = paymentRepository.findByOrderIdAndPaymentStatus(approvalReq.orderId(), PaymentStatus.PENDING)
                .orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));

        if(!payment.isSameRequestedAmount(approvalReq.requestedAmount())) throw new BusinessException(PAYMENT_INVALID);
        if(!payment.isSameMethod(approvalReq.method())) throw new BusinessException(PAYMENT_INVALID);
        if(!payment.isSameProvider(approvalReq.provider())) throw new  BusinessException(PAYMENT_INVALID);

        payment.completeValidate(approvalReq);
        log.info("검증 성공 상태 = {}", payment.getPaymentStatus());
        return payment.getPaymentPayload();
    }

    public Result approve(PaymentPayload paymentPayload) {
        log.info("Toss Payment 호출 전 승인 예정 금액 = {}", paymentPayload.getAmount());
        return tossPayment.call(paymentPayload);
    }

    @Transactional
    public Payment finalizePaymentPayload(Result approveResult) {
        if(approveResult instanceof SuccessResult successResult) {
            log.info("Toss Payment 호출 성공");
            return applySuccessResult(successResult);
        } else if (approveResult instanceof FailedResult failedResult) {
            log.info("Toss Payment 호출 실패");
            return applyFailedResult(failedResult);
        }
        throw new BusinessException(UNKNOWN_ERROR);
    }

    public void recoverApproveFailed(PaymentPayload paymentPayload) {
        Payment retryFailedPayment = paymentRepository.findByOrderIdAndPaymentStatus(paymentPayload.getOrderId(),
                PaymentStatus.APPROVING).orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));
        retryFailedPayment.retryFailed();
        log.info("재시도 실패 상태 = {}", retryFailedPayment.getPaymentStatus());
    }

    private Payment applySuccessResult(SuccessResult successResult) {
        Payment successedPayment = paymentRepository.findByOrderIdAndPaymentStatus(successResult.orderId(),
                PaymentStatus.APPROVING).orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));
        successedPayment.success(successResult);
        log.info("승인 금액 = {} 상태 = {}", successedPayment.getApprovedAmount(), successedPayment.getPaymentStatus());
        return successedPayment;
    }

    private Payment applyFailedResult(FailedResult failedResult) {
        Payment failedPayment = paymentRepository.findByOrderIdAndPaymentStatus(failedResult.orderId(),
                PaymentStatus.APPROVING).orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));
        failedPayment.failed(failedResult);
        log.info("실패 메시지 = {} 실패 상태 = {}", failedPayment.getFailedMessage(), failedPayment.getPaymentStatus());
        return failedPayment;
    }
}