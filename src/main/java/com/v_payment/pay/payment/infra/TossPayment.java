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
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j(topic = "API_LOGGER")
@Component
@RequiredArgsConstructor
public class TossPayment {
    private static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    private static final String CONTENT_TYPE_HEADER_KEY = "Content-Type";

    private final RestClient tossPaymentClient;
    private final TossPaymentProperties tossPaymentProperties;

    @Timed(value = "pay.api")
    public Result call(PaymentPayload paymentPayload) {
        long callStartTime = LTimer.getCurrTime();
        Result result = null;

        try{
            result = exchangeRequestToResponse(paymentPayload);
            return result;
        } catch (ResourceAccessException e) {
            result = new FailedResult(paymentPayload.getOrderId(), PaymentError.NETWORK_TIMEOUT, e.getMessage());
            return result;
        } catch (RestClientResponseException e) {
            int statusCode = e.getStatusCode().value();
            if(statusCode == 429) {
                result = new FailedResult(paymentPayload.getOrderId(), PaymentError.UPSTREAM_429, e.getMessage());
                return result;
            }
            if(statusCode >= 500) {
                result = new FailedResult(paymentPayload.getOrderId(), PaymentError.UPSTREAM_5XX, e.getMessage());
                return result;
            }
            result = new  FailedResult(paymentPayload.getOrderId(), PaymentError.UPSTREAM_4XX, e.getMessage());
            return result;
        } catch (RuntimeException e) {
            result = new FailedResult(paymentPayload.getOrderId(), PaymentError.UNKNOWN, e.getMessage());
            return result;
        } finally {
            if(result instanceof FailedResult failedResult) {
                log.info("승인 API 호출 실패 orderId = {} 원인 = {} elapsedMs = {}", paymentPayload.getOrderId(), failedResult.message(), LTimer.getDiff(callStartTime));
            }else {
                log.info("승인 API 호출 성공 orderId = {} elapsedMs = {}", paymentPayload.getOrderId(), LTimer.getDiff(callStartTime));
            }
            if(LTimer.getDiff(callStartTime) > tossPaymentProperties.timeout() * 1000) {
                log.warn("승인 API 호출이 느립니다. orderId = {} elapsedMs = {}",  paymentPayload.getOrderId(), LTimer.getDiff(callStartTime));
            }
        }
    }

    private Result exchangeRequestToResponse(PaymentPayload paymentPayload) {
        return tossPaymentClient.post()
                .uri(tossPaymentProperties.uri())
                .header(AUTHORIZATION_HEADER_KEY, encodeBase64(tossPaymentProperties.secret()))
                .header(CONTENT_TYPE_HEADER_KEY, tossPaymentProperties.contentType())
                .body(paymentPayload)
                .retrieve()
                .body(SuccessResult.class);
    }

    private String encodeBase64(String secretKey) {
        return "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }
}
