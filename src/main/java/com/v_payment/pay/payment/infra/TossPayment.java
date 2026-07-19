package com.v_payment.pay.payment.infra;

import com.v_payment.pay.global.LTimer;
import com.v_payment.pay.payment.config.TossPaymentProperties;
import com.v_payment.pay.payment.entity.PaymentPayload;
import io.micrometer.core.annotation.Timed;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j(topic = "SCHEDULER_LOGGER")
@Component
@RequiredArgsConstructor
public class TossPayment {
    private static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    private static final String CONTENT_TYPE_HEADER_KEY = "Content-Type";
    private static final String IDEMPOTENCY_KEY_HEADER_KEY = "Idempotency-Key";

    private final RestClient tossPaymentClient;
    private final TossPaymentProperties tossPaymentProperties;

    @Timed(value = "pay.api")
    @WithSpan("payment.toss.approve")
    public Result approve(PaymentPayload paymentPayload) {
        long callStartTime = LTimer.getCurrTime();

        try {
            return exchangeApprove(paymentPayload);
        } catch (ResourceAccessException e) {
            log.warn("approval API timeout. orderCode = {} elapsedMs = {} error = {}",
                    paymentPayload.getOrderCode(), LTimer.getDiff(callStartTime), e.toString());
            return new FailedResult(paymentPayload.getOrderCode(), PaymentError.NETWORK_TIMEOUT, e.getMessage());
        } catch (RestClientResponseException e) {
            log.warn("approval API failed. orderCode = {} elapsedMs = {} error = {}",
                    paymentPayload.getOrderCode(), LTimer.getDiff(callStartTime), e.toString());
            int statusCode = e.getStatusCode().value();
            if (statusCode == 429) return new FailedResult(paymentPayload.getOrderCode(), PaymentError.UPSTREAM_429, e.getMessage());
            if (statusCode >= 500) return new FailedResult(paymentPayload.getOrderCode(), PaymentError.UPSTREAM_5XX, e.getMessage());
            return new FailedResult(paymentPayload.getOrderCode(), PaymentError.UPSTREAM_4XX, e.getMessage());
        } catch (RuntimeException e) {
            log.warn("approval API failed. orderCode = {} elapsedMs = {} error = {}",
                    paymentPayload.getOrderCode(), LTimer.getDiff(callStartTime), e.toString());
            return new FailedResult(paymentPayload.getOrderCode(), PaymentError.UNKNOWN, e.getMessage());
        }
    }

    @WithSpan("payment.toss.exchange_approve")
    private Result exchangeApprove(PaymentPayload paymentPayload) {
        return tossPaymentClient.post()
                .uri(tossPaymentProperties.uri())
                .header(AUTHORIZATION_HEADER_KEY, encodeBase64(tossPaymentProperties.secret()))
                .header(CONTENT_TYPE_HEADER_KEY, tossPaymentProperties.contentType())
                .header(IDEMPOTENCY_KEY_HEADER_KEY, paymentPayload.getOrderCode())
                .body(paymentPayload)
                .retrieve()
                .body(SuccessResult.class);
    }

    @WithSpan("payment.toss.encode_auth")
    private String encodeBase64(String secretKey) {
        return "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }
}
