package com.v_payment.pay.payment.service;

import com.v_payment.pay.global.exception.BusinessException;
import com.v_payment.pay.order.service.OrderManager;
import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.res.ApprovalRes;
import com.v_payment.pay.payment.domain.entity.Payment;
import com.v_payment.pay.payment.domain.entity.PaymentPayload;
import com.v_payment.pay.payment.domain.entity.PaymentStatus;
import com.v_payment.pay.payment.infra.result.AbortedResult;
import com.v_payment.pay.payment.infra.result.DoneResult;
import com.v_payment.pay.payment.infra.result.ExpiredResult;
import com.v_payment.pay.payment.infra.result.Result;
import com.v_payment.pay.payment.infra.result.UnknownResult;
import com.v_payment.pay.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.v_payment.pay.payment.exception.PaymentException.PAYMENT_NOT_FOUND;
import static com.v_payment.pay.payment.exception.PaymentException.PAYMENT_INVALID;
import static com.v_payment.pay.payment.exception.PaymentException.UNKNOWN_ERROR;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApprovalService {
    private final PaymentRepository paymentRepository;
    private final OrderManager orderManager;

    @Transactional
    public PaymentPayload validateApprovalReq(ApprovalReq approvalReq) {
        Payment payment = paymentRepository.findByIdempotencyKey(approvalReq.idempotencyKey())
                .orElseThrow(() -> new BusinessException(PAYMENT_NOT_FOUND));

        if (!payment.isReady()) throw new BusinessException(PAYMENT_INVALID);
        if (!payment.isSameRequestedAmount(approvalReq.requestedAmount())) throw new BusinessException(PAYMENT_INVALID);
        if (!payment.isSameProvider(approvalReq.provider())) throw new BusinessException(PAYMENT_INVALID);
        if (!payment.isSamePaymentMethod(approvalReq.method())) throw new BusinessException(PAYMENT_INVALID);

        payment.markInProgress(approvalReq.paymentKey());

        return PaymentPayload.create(payment.getOrderCode(), payment.getIdempotencyKey(), approvalReq.paymentKey(),
                approvalReq.requestedAmount());
    }

    @Transactional
    public ApprovalRes finalizePaymentPayload(Result approveResult) {
        if (approveResult instanceof DoneResult doneResult) {
            return applyDoneResult(doneResult);
        }
        if (approveResult instanceof AbortedResult abortedResult) {
            return applyAbortedResult(abortedResult);
        }
        if (approveResult instanceof UnknownResult unknownResult) {
            return applyUnknownResult(unknownResult);
        }
        if (approveResult instanceof ExpiredResult expiredResult) {
            return applyExpiredResult(expiredResult);
        }
        throw new BusinessException(UNKNOWN_ERROR);
    }

    private ApprovalRes applyDoneResult(DoneResult doneResult) {
        int updatedRows = paymentRepository.markDone(
                doneResult.idempotencyKey(),
                PaymentStatus.IN_PROGRESS,
                PaymentStatus.UNKNOWN,
                PaymentStatus.DONE,
                doneResult.totalAmount(),
                doneResult.approvedAt(),
                doneResult.receipt() == null ? null : doneResult.receipt().url()
        );
        if (updatedRows != 1) throw new BusinessException(PAYMENT_NOT_FOUND);

        applyOrderPaymentDone(doneResult.orderCode());
        return ApprovalRes.from(doneResult);
    }

    private ApprovalRes applyAbortedResult(AbortedResult abortedResult) {
        int updatedRows = paymentRepository.markAborted(
                abortedResult.idempotencyKey(),
                PaymentStatus.IN_PROGRESS,
                PaymentStatus.UNKNOWN,
                PaymentStatus.ABORTED
        );
        if (updatedRows != 1) throw new BusinessException(PAYMENT_NOT_FOUND);

        applyOrderPaymentFailed(abortedResult.orderCode());
        return ApprovalRes.from(abortedResult);
    }

    private ApprovalRes applyUnknownResult(UnknownResult unknownResult) {
        int updatedRows = paymentRepository.markUnknown(
                unknownResult.idempotencyKey(),
                PaymentStatus.IN_PROGRESS,
                PaymentStatus.UNKNOWN
        );
        if (updatedRows != 1) throw new BusinessException(PAYMENT_NOT_FOUND);

        return ApprovalRes.from(unknownResult);
    }

    private ApprovalRes applyExpiredResult(ExpiredResult expiredResult) {
        int updatedRows = paymentRepository.markExpired(
                expiredResult.idempotencyKey(),
                PaymentStatus.IN_PROGRESS,
                PaymentStatus.UNKNOWN,
                PaymentStatus.EXPIRED
        );
        if (updatedRows != 1) throw new BusinessException(PAYMENT_NOT_FOUND);

        applyOrderPaymentExpired(expiredResult.orderCode());
        return ApprovalRes.from(expiredResult);
    }

    private void applyOrderPaymentDone(String orderCode) {
        boolean updated = orderManager.markPaid(orderCode);
        if (!updated) log.error("결제 완료 후 주문 결제완료 상태 업데이트를 실패했습니다. orderCode={}", orderCode);
    }

    private void applyOrderPaymentFailed(String orderCode) {
        boolean updated = orderManager.markLackQuantity(orderCode);
        if (!updated) log.error("결제 실패 후 주문 재고부족 상태 업데이트를 실패했습니다. orderCode={}", orderCode);
    }

    private void applyOrderPaymentExpired(String orderCode) {
        boolean updated = orderManager.markExpired(orderCode);
        if (!updated) log.error("결제 만료 후 주문 만료 상태 업데이트를 실패했습니다. orderCode={}", orderCode);
    }
}
