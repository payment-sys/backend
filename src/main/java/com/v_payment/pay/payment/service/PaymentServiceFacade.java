package com.v_payment.pay.payment.service;

import com.v_payment.pay.global.ExecutorWithRetry;
import com.v_payment.pay.payment.controller.dto.req.ApprovalReq;
import com.v_payment.pay.payment.controller.dto.res.ApprovalRes;
import com.v_payment.pay.payment.entity.Payment;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.infra.FailedResult;
import com.v_payment.pay.payment.infra.PaymentError;
import com.v_payment.pay.payment.infra.Result;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j(topic = "API_LOGGER")
@Component
@RequiredArgsConstructor
public class PaymentServiceFacade {
    private final PaymentService paymentService;
    private final MeterRegistry meterRegistry;

    @Timed(value = "payment.tx.approve")
    public ApprovalRes approvePipeline(ApprovalReq approvalReq) {
        PaymentPayload paymentPayload;
        Result approveResult;
        Payment finishedPayment;

        Timer.Sample validateSample = Timer.start(meterRegistry);
        try {
            long s = System.currentTimeMillis();
            paymentPayload = paymentService.validateApprovalReq(approvalReq);
            long e = System.currentTimeMillis();
            log.info("validate 실행시간 = {}", e - s);
        } finally {
            validateSample.stop(
                    Timer.builder("payment.tx.validate1")
                            .register(meterRegistry)
            );
        }

        Timer.Sample pgSample = Timer.start(meterRegistry);
        try {
            approveResult = getApproveResult(paymentPayload);
        } finally {
            pgSample.stop(
                    Timer.builder("payment.external.pg")
                            .register(meterRegistry)
            );
        }

        Timer.Sample finalizeSample = Timer.start(meterRegistry);
        try {
            finishedPayment = paymentService.finalizePaymentPayload(approveResult);
        } finally {
            finalizeSample.stop(
                    Timer.builder("payment.tx.finalize1")
                            .register(meterRegistry)
            );
        }

        log.info("승인 성공");
        return ApprovalRes.from(finishedPayment);
    }

    private Result getApproveResult(PaymentPayload paymentPayload) {
        return ExecutorWithRetry
                .task(() -> paymentService.approve(paymentPayload))
                .continueCondition(this::getContinueCondition)
                .delayMillis(1000)
                .maxAttempts(3)
                .recovery(() -> paymentService.recoverApproveFailed(paymentPayload))
                .execute();
    }

    private boolean getContinueCondition(Result result) {
        if(result instanceof FailedResult failedResult) {
            return failedResult.paymentError() == PaymentError.NETWORK_TIMEOUT ||
                    failedResult.paymentError() == PaymentError.UPSTREAM_429 ||
                    failedResult.paymentError() == PaymentError.UPSTREAM_5XX;
        }
        return false;
    }
}
