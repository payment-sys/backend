package com.v_payment.pay.payment.infra.toss;

import com.v_payment.pay.payment.infra.*;
import com.v_payment.pay.payment.infra.result.*;
import org.springframework.stereotype.Component;

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
