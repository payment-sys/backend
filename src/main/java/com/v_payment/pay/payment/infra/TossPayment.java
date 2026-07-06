package com.v_payment.pay.payment.infra;

import com.v_payment.pay.global.LTimer;
import com.v_payment.pay.payment.config.TossPaymentProperties;
import com.v_payment.pay.payment.entity.PaymentPayload;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

@Slf4j(topic = "SCHEDULER_LOGGER")
@Component
@RequiredArgsConstructor
public class TossPayment implements Pay {
    private static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    private static final String CONTENT_TYPE_HEADER_KEY = "Content-Type";
    private static final String IDEMPOTENCY_KEY_HEADER_KEY = "Idempotency-Key";
    private static final String CONFIRM_URI_SUFFIX = "/confirm";

    private final RestClient tossPaymentClient;
    private final TossPaymentProperties tossPaymentProperties;

    @Override
    @Timed(value = "pay.api")
    public Result approve(PaymentPayload paymentPayload) {
        long callStartTime = LTimer.getCurrTime();

        try {
            return exchangeRequestToResponse(paymentPayload);
        } catch (ResourceAccessException e) {
            log.warn("Payment confirm request failed. orderId = {} elapsedMs = {} error = {}",
                    paymentPayload.getOrderId(), LTimer.getDiff(callStartTime), e.toString());
            return check(paymentPayload, PaymentError.NETWORK_TIMEOUT, e.getMessage());
        } catch (RestClientResponseException e) {
            log.warn("Payment confirm request failed. orderId = {} elapsedMs = {} status = {} error = {}",
                    paymentPayload.getOrderId(), LTimer.getDiff(callStartTime), e.getStatusCode().value(), e.toString());
            int statusCode = e.getStatusCode().value();
            if (isCheckRequired(statusCode)) {
                return check(paymentPayload, resolvePaymentError(statusCode), e.getMessage());
            }
            return new FailedResult(paymentPayload.getOrderId(), PaymentError.UPSTREAM_4XX, e.getMessage());
        } catch (RestClientException e) {
            log.warn("Payment confirm request failed. orderId = {} elapsedMs = {} error = {}",
                    paymentPayload.getOrderId(), LTimer.getDiff(callStartTime), e.toString());
            return check(paymentPayload, PaymentError.UNKNOWN, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("Unexpected payment confirm failure. orderId = {} elapsedMs = {} error = {}",
                    paymentPayload.getOrderId(), LTimer.getDiff(callStartTime), e.toString());
            return new RetryableResult(paymentPayload.getOrderId(), PaymentError.UNKNOWN, e.getMessage());
        }
    }

    @Override
    public Result check(PaymentPayload paymentPayload) {
        return check(paymentPayload, PaymentError.UNKNOWN, "payment status is not confirmed");
    }

    private Result check(PaymentPayload paymentPayload,
                         PaymentError paymentError,
                         String message) {
        try {
            SuccessResult successResult = exchangeCheckRequestToResponse(paymentPayload);
            return convertCheckResult(paymentPayload, successResult, paymentError, message);
        } catch (ResourceAccessException | RestClientResponseException e) {
            log.warn("Payment check failed. orderId = {} error = {}", paymentPayload.getOrderId(), e.toString());
            return new RetryableResult(paymentPayload.getOrderId(), paymentError, message);
        } catch (RuntimeException e) {
            log.warn("Payment check failed. orderId = {} error = {}", paymentPayload.getOrderId(), e.toString());
            return new RetryableResult(paymentPayload.getOrderId(), paymentError, message);
        }
    }

    private Result exchangeRequestToResponse(PaymentPayload paymentPayload) {
        return tossPaymentClient.post()
                .uri(tossPaymentProperties.uri())
                .header(AUTHORIZATION_HEADER_KEY, encodeBase64(tossPaymentProperties.secret()))
                .header(CONTENT_TYPE_HEADER_KEY, tossPaymentProperties.contentType())
                .header(IDEMPOTENCY_KEY_HEADER_KEY, createIdempotencyKey(paymentPayload))
                .body(paymentPayload)
                .retrieve()
                .body(SuccessResult.class);
    }

    private SuccessResult exchangeCheckRequestToResponse(PaymentPayload paymentPayload) {
        return tossPaymentClient.get()
                .uri(createCheckUri(paymentPayload))
                .header(AUTHORIZATION_HEADER_KEY, encodeBase64(tossPaymentProperties.secret()))
                .retrieve()
                .body(SuccessResult.class);
    }

    private Result convertCheckResult(PaymentPayload paymentPayload,
                                      SuccessResult successResult,
                                      PaymentError paymentError,
                                      String message) {
        if (successResult == null || successResult.status() == null) {
            return new RetryableResult(paymentPayload.getOrderId(), paymentError, message);
        }

        String status = successResult.status().toUpperCase(Locale.ROOT);
        if ("DONE".equals(status)) {
            return successResult;
        }
        if ("ABORTED".equals(status) || "EXPIRED".equals(status) || "CANCELED".equals(status)) {
            return new FailedResult(paymentPayload.getOrderId(), PaymentError.UPSTREAM_4XX,
                    "payment status = " + status);
        }
        return new RetryableResult(paymentPayload.getOrderId(), paymentError,
                "payment status = " + status);
    }

    private boolean isCheckRequired(int statusCode) {
        return statusCode == 408 || statusCode == 429 || statusCode >= 500;
    }

    private PaymentError resolvePaymentError(int statusCode) {
        if (statusCode == 429) {
            return PaymentError.UPSTREAM_429;
        }
        if (statusCode >= 500) {
            return PaymentError.UPSTREAM_5XX;
        }
        return PaymentError.UNKNOWN;
    }

    private String encodeBase64(String secretKey) {
        return "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }

    private String createIdempotencyKey(PaymentPayload paymentPayload) {
        return "confirm:" + paymentPayload.getOrderId();
    }

    private String createCheckUri(PaymentPayload paymentPayload) {
        String paymentUri = createPaymentUri();
        if (paymentPayload.getPaymentKey() != null && !paymentPayload.getPaymentKey().isBlank()) {
            return paymentUri + "/" + paymentPayload.getPaymentKey();
        }
        return paymentUri + "/orders/" + paymentPayload.getOrderId();
    }

    private String createPaymentUri() {
        String confirmUri = tossPaymentProperties.uri();
        if (confirmUri.endsWith(CONFIRM_URI_SUFFIX)) {
            return confirmUri.substring(0, confirmUri.length() - CONFIRM_URI_SUFFIX.length());
        }
        return confirmUri;
    }
}
