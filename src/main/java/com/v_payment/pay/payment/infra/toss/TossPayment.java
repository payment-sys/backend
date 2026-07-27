package com.v_payment.pay.payment.infra.toss;

import com.v_payment.pay.payment.config.TossPaymentProperties;
import com.v_payment.pay.payment.entity.PaymentPayload;
import com.v_payment.pay.payment.infra.*;
import com.v_payment.pay.payment.infra.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
        return tossPaymentClient.post()
                .uri(tossPaymentProperties.uri())
                .header(AUTHORIZATION_HEADER_KEY, encodeBase64(tossPaymentProperties.secret()))
                .header(CONTENT_TYPE_HEADER_KEY, tossPaymentProperties.contentType())
                .header(IDEMPOTENCY_KEY_HEADER_KEY, paymentPayload.getOrderCode())
                .body(paymentPayload)
                .exchange((req, res) -> {
                    if (res.getStatusCode().is2xxSuccessful()) {
                        return objectMapper.readValue(res.getBody(), TossPaymentConfirmSuccessRes.class);
                    }

                    TossPaymentConfirmErrorRes errorRes = objectMapper.readValue(
                            res.getBody(),
                            TossPaymentConfirmErrorRes.class
                    );
                    return new TossPaymentConfirmErrorRes(
                            res.getStatusCode().value(),
                            errorRes.code(),
                            errorRes.message()
                    );
                });
    }

    private String encodeBase64(String secretKey) {
        return "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }
}
