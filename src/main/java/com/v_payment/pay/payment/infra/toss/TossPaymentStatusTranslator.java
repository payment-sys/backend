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
    public Result translate(PaymentConfirmRes paymentConfirmRes, String orderCode, String idempotencyKey) {
        if (paymentConfirmRes instanceof TossPaymentConfirmErrorRes errorRes) {
            return mapError(errorRes, orderCode, idempotencyKey);
        }
        if (paymentConfirmRes instanceof TossPaymentConfirmSuccessRes successRes) {
            return mapSuccess(successRes, orderCode, idempotencyKey);
        }

        return mapUnknown(orderCode, idempotencyKey, "Unsupported payment confirm response");
    }

    public Result translateTimeout(String orderCode, String idempotencyKey, String message) {
        return new UnknownResult(orderCode, idempotencyKey, PaymentError.NETWORK_TIMEOUT, message);
    }

    public Result translateUnknown(String orderCode, String idempotencyKey, String message) {
        return mapUnknown(orderCode, idempotencyKey, message);
    }

    private Result mapSuccess(TossPaymentConfirmSuccessRes successRes, String orderCode, String idempotencyKey) {
        if (successRes.status() == null) {
            return mapUnknown(orderCode, idempotencyKey, "Toss payment status is null");
        }

        return switch (successRes.status()) {
            case DONE -> mapDone(successRes, orderCode, idempotencyKey);
            case ABORTED -> mapAborted(orderCode, idempotencyKey, "Toss payment status is ABORTED");
            case EXPIRED -> mapExpired(orderCode, idempotencyKey, "Toss payment status is EXPIRED");
            default -> mapUnknown(orderCode, idempotencyKey, "Unsupported toss payment status: " + successRes.status());
        };
    }

    private Result mapError(TossPaymentConfirmErrorRes errorRes, String orderCode, String idempotencyKey) {
        if (errorRes.httpStatusCode() != null && errorRes.httpStatusCode() == 429) {
            return new UnknownResult(orderCode, idempotencyKey, PaymentError.UPSTREAM_429, errorRes.message());
        }
        if (errorRes.httpStatusCode() != null && errorRes.httpStatusCode() >= 500) {
            return new UnknownResult(orderCode, idempotencyKey, PaymentError.UPSTREAM_5XX, errorRes.message());
        }
        if (NOT_FOUND_PAYMENT_SESSION.equals(errorRes.code())) {
            return mapExpired(orderCode, idempotencyKey, errorRes.message());
        }
        return mapAborted(orderCode, idempotencyKey, errorRes.message());
    }

    private DoneResult mapDone(TossPaymentConfirmSuccessRes successRes, String orderCode, String idempotencyKey) {
        return new DoneResult(
                orderCode,
                idempotencyKey,
                successRes.paymentKey(),
                successRes.totalAmount(),
                successRes.approvedAt() == null ? null : successRes.approvedAt().toLocalDateTime(),
                successRes.receipt() == null ? null : new DoneResult.Receipt(successRes.receipt().url())
        );
    }

    private AbortedResult mapAborted(String orderCode, String idempotencyKey, String message) {
        return new AbortedResult(orderCode, idempotencyKey, PaymentError.UPSTREAM_4XX, message);
    }

    private ExpiredResult mapExpired(String orderCode, String idempotencyKey, String message) {
        return new ExpiredResult(orderCode, idempotencyKey, PaymentError.UPSTREAM_4XX, message);
    }

    private UnknownResult mapUnknown(String orderCode, String idempotencyKey, String message) {
        return new UnknownResult(orderCode, idempotencyKey, PaymentError.UNKNOWN, message);
    }
}
