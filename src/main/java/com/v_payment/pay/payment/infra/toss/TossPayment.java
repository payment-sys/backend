package com.v_payment.pay.payment.infra.toss;

import com.v_payment.pay.payment.config.TossPaymentProperties;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.infra.*;
import com.v_payment.pay.payment.infra.result.Result;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j(topic = "SCHEDULER_LOGGER")
@Component
@RequiredArgsConstructor
public class TossPayment {
    private static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    private static final String CONTENT_TYPE_HEADER_KEY = "Content-Type";
    private static final String IDEMPOTENCY_KEY_HEADER_KEY = "Idempotency-Key";

    private final ObjectMapper objectMapper;
    private final RestClient tossPaymentClient;
    private final TossPaymentStatusTranslator tossPaymentStatusTranslator;
    private final TossPaymentProperties tossPaymentProperties;
    private final MeterRegistry meterRegistry;

    public Result approve(PaymentPayload paymentPayload) {
        try {
            PaymentConfirmRes paymentConfirmRes = exchangeApprove(paymentPayload);
            return tossPaymentStatusTranslator.translate(paymentConfirmRes, paymentPayload.getOrderCode());
        } catch (ResourceAccessException e) {
            return tossPaymentStatusTranslator.translateTimeout(paymentPayload.getOrderCode(), e.getMessage());
        } catch (RuntimeException e) {
            return tossPaymentStatusTranslator.translateUnknown(paymentPayload.getOrderCode(), e.getMessage());
        }
    }

    private PaymentConfirmRes exchangeApprove(PaymentPayload paymentPayload) {
        long startedAtNanos = System.nanoTime();
        AtomicReference<String> status = new AtomicReference<>("UNKNOWN");
        AtomicReference<String> responseOutcome = new AtomicReference<>("SUCCESS");
        String outcome = "SUCCESS";
        String error = "none";

        try {
            return tossPaymentClient.post()
                    .uri(tossPaymentProperties.uri())
                    .header(AUTHORIZATION_HEADER_KEY, encodeBase64(tossPaymentProperties.secret()))
                    .header(CONTENT_TYPE_HEADER_KEY, tossPaymentProperties.contentType())
                    .header(IDEMPOTENCY_KEY_HEADER_KEY, paymentPayload.getOrderCode())
                    .body(paymentPayload)
                    .exchange((req, res) -> {
                        HttpStatusCode statusCode = res.getStatusCode();
                        status.set(String.valueOf(statusCode.value()));
                        responseOutcome.set(outcome(statusCode));
                        if (statusCode.is2xxSuccessful()) {
                            return objectMapper.readValue(res.getBody(), TossPaymentConfirmSuccessRes.class);
                        }

                        TossPaymentConfirmErrorRes errorRes = objectMapper.readValue(
                                res.getBody(),
                                TossPaymentConfirmErrorRes.class
                        );
                        return new TossPaymentConfirmErrorRes(
                                statusCode.value(),
                                errorRes.code(),
                                errorRes.message()
                        );
                    });
        } catch (ResourceAccessException e) {
            outcome = "ERROR";
            error = "ResourceAccessException";
            throw e;
        } catch (RuntimeException e) {
            outcome = "ERROR";
            error = e.getClass().getSimpleName();
            throw e;
        } finally {
            recordExternalRequest(status.get(), "SUCCESS".equals(outcome) ? responseOutcome.get() : outcome, error,
                    startedAtNanos);
        }
    }

    private String encodeBase64(String secretKey) {
        return "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }

    private void recordExternalRequest(String status, String outcome, String error, long startedAtNanos) {
        Timer.builder("pay.external.requests.duration")
                .description("External API request duration")
                .tag("client", "toss")
                .tag("operation", "confirm")
                .tag("method", "POST")
                .tag("status", status)
                .tag("outcome", outcome)
                .tag("error", error)
                .register(meterRegistry)
                .record(System.nanoTime() - startedAtNanos, TimeUnit.NANOSECONDS);
    }

    private String outcome(HttpStatusCode statusCode) {
        if (statusCode.is2xxSuccessful()) return "SUCCESS";
        if (statusCode.is4xxClientError()) return "CLIENT_ERROR";
        if (statusCode.is5xxServerError()) return "SERVER_ERROR";
        return "UNKNOWN";
    }
}
