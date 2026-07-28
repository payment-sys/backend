package com.v_payment.pay.payment.infra.toss;

import com.v_payment.pay.payment.infra.*;
import com.v_payment.pay.payment.infra.result.*;
import org.springframework.stereotype.Component;

/**
 * unit test
 *
 * DisplayName: 응답이 존재하고, httpStatusCode가 429일 경우 translate 호출 시 UnknownResult를 반환한다.
 * Given: httpStatusCode가 429인 TossPaymentConfirmErrorRes와 fallbackOrderCode가 준비되어 있다.
 * When: translate(errorRes, fallbackOrderCode)를 호출한다.
 * Then: UnknownResult를 반환하고, paymentError는 UPSTREAM_429이며, orderCode와 message가 유지된다.
 *
 * DisplayName: 응답이 존재하고, httpStatusCode가 500 이상일 경우 translate 호출 시 UnknownResult를 반환한다.
 * Given: httpStatusCode가 500 이상인 TossPaymentConfirmErrorRes와 fallbackOrderCode가 준비되어 있다.
 * When: translate(errorRes, fallbackOrderCode)를 호출한다.
 * Then: UnknownResult를 반환하고, paymentError는 UPSTREAM_5XX이며, orderCode와 message가 유지된다.
 *
 * DisplayName: 응답이 존재하고, code가 "NOT_FOUND_PAYMENT_SESSION"일 경우 translate 호출 시 ExpiredResult를 반환한다.
 * Given: code가 "NOT_FOUND_PAYMENT_SESSION"인 TossPaymentConfirmErrorRes와 fallbackOrderCode가 준비되어 있다.
 * When: translate(errorRes, fallbackOrderCode)를 호출한다.
 * Then: ExpiredResult를 반환하고, paymentError는 UPSTREAM_4XX이며, orderCode와 message가 유지된다.
 *
 * DisplayName: 응답이 존재하고, httpStatusCode가 500 미만, 429도 아니고, code가 "NOT_FOUND_PAYMENT_SESSION"이 아닌 경우 translate 호출 시 AbortedResult를 반환한다.
 * Given: httpStatusCode가 500 미만이고 429가 아니며 code가 "NOT_FOUND_PAYMENT_SESSION"이 아닌 TossPaymentConfirmErrorRes와 fallbackOrderCode가 준비되어 있다.
 * When: translate(errorRes, fallbackOrderCode)를 호출한다.
 * Then: AbortedResult를 반환하고, paymentError는 UPSTREAM_4XX이며, orderCode와 message가 유지된다.
 *
 * DisplayName: 응답이 존재하고, 승인 성공 후 translate 호출 시 DoneResult를 반환한다.
 * Given: status가 "DONE"인 TossPaymentConfirmSuccessRes와 fallbackOrderCode가 준비되어 있다.
 * When: translate(successRes, fallbackOrderCode)를 호출한다.
 * Then: DoneResult를 반환하고, orderCode, paymentKey, totalAmount, approvedAt, receipt가 성공 응답 값과 일치한다.
 *
 * DisplayName: 응답이 존재하고, 승인 성공 응답을 받았지만 status가 존재하지 않는다면 UnknownResult를 반환한다.
 * Given: status가 null인 TossPaymentConfirmSuccessRes와 fallbackOrderCode가 준비되어 있다.
 * When: translate(successRes, fallbackOrderCode)를 호출한다.
 * Then: UnknownResult를 반환하고, paymentError는 UNKNOWN이며, message는 "Toss payment status is null"이다.
 *
 * DisplayName: 응답이 존재하지 않고, 타임아웃일 때 translateTimeout 호출 시 UnknownResult를 반환한다.
 * Given: fallbackOrderCode와 timeout message가 준비되어 있다.
 * When: translateTimeout(fallbackOrderCode, message)를 호출한다.
 * Then: UnknownResult를 반환하고, paymentError는 NETWORK_TIMEOUT이며, orderCode와 message가 유지된다.
 *
 * DisplayName: 응답이 존재하지 않고, 알 수 없는 예외일 때 translateUnknown 호출 시 UnknownResult를 반환한다.
 * Given: fallbackOrderCode와 exception message가 준비되어 있다.
 * When: translateUnknown(fallbackOrderCode, message)를 호출한다.
 * Then: UnknownResult를 반환하고, paymentError는 UNKNOWN이며, orderCode와 message가 유지된다.
 */

@Component
public class TossPaymentStatusTranslator implements PaymentStatusTranslator {
    private static final String DONE = "DONE";
    private static final String ABORTED = "ABORTED";
    private static final String EXPIRED = "EXPIRED";
    private static final String NOT_FOUND_PAYMENT_SESSION = "NOT_FOUND_PAYMENT_SESSION";

    @Override
    public Result translate(PaymentConfirmRes paymentConfirmRes, String fallbackOrderCode) {
        if (paymentConfirmRes instanceof TossPaymentConfirmErrorRes errorRes) {
            return mapError(errorRes, fallbackOrderCode);
        }
        if (paymentConfirmRes instanceof TossPaymentConfirmSuccessRes successRes) {
            return mapSuccess(successRes, fallbackOrderCode);
        }

        return mapUnknown(fallbackOrderCode, "Unsupported payment confirm response");
    }

    public Result translateTimeout(String fallbackOrderCode, String message) {
        return new UnknownResult(fallbackOrderCode, PaymentError.NETWORK_TIMEOUT, message);
    }

    public Result translateUnknown(String fallbackOrderCode, String message) {
        return mapUnknown(fallbackOrderCode, message);
    }

    private Result mapSuccess(TossPaymentConfirmSuccessRes successRes, String fallbackOrderCode) {
        String orderCode = resolveOrderCode(successRes.orderCode(), fallbackOrderCode);
        if (successRes.status() == null) return mapUnknown(orderCode, "Toss payment status is null");
        return mapDone(successRes, orderCode);
    }

    private Result mapError(TossPaymentConfirmErrorRes errorRes, String fallbackOrderCode) {
        if (errorRes.httpStatusCode() != null && errorRes.httpStatusCode() == 429) {
            return new UnknownResult(fallbackOrderCode, PaymentError.UPSTREAM_429, errorRes.message());
        }
        if (errorRes.httpStatusCode() != null && errorRes.httpStatusCode() >= 500) {
            return new UnknownResult(fallbackOrderCode, PaymentError.UPSTREAM_5XX, errorRes.message());
        }
        if (NOT_FOUND_PAYMENT_SESSION.equals(errorRes.code())) {
            return mapExpired(fallbackOrderCode, errorRes.message());
        }
        return mapAborted(fallbackOrderCode, errorRes.message());
    }

    private DoneResult mapDone(TossPaymentConfirmSuccessRes successRes, String orderCode) {
        return new DoneResult(
                orderCode,
                successRes.paymentKey(),
                successRes.totalAmount(),
                successRes.approvedAt() == null ? null : successRes.approvedAt().toLocalDateTime(),
                successRes.receipt() == null ? null : new DoneResult.Receipt(successRes.receipt().url())
        );
    }

    private AbortedResult mapAborted(String orderCode, String message) {
        return new AbortedResult(orderCode, PaymentError.UPSTREAM_4XX, message);
    }

    private ExpiredResult mapExpired(String orderCode, String message) {
        return new ExpiredResult(orderCode, PaymentError.UPSTREAM_4XX, message);
    }

    private UnknownResult mapUnknown(String orderCode, String message) {
        return new UnknownResult(orderCode, PaymentError.UNKNOWN, message);
    }

    private String resolveOrderCode(String responseOrderCode, String fallbackOrderCode) {
        return responseOrderCode == null ? fallbackOrderCode : responseOrderCode;
    }
}
