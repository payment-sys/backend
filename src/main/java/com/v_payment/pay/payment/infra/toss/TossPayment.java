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

/**
 * unit test
 * DisplayName: tossPaymentClient가 2XX성공 응답 반환 시 translator.translate()를 호출한다.
 * Given: tossPaymentClient가 TossPaymentConfirmSuccessRes 반환 stub
 * When: approve() 호출
 * Then: translateor.translate() 호출해야함
 *
 * DisplayName: tossPaymentClient가 4xx/5xx 에러 응답 반환 시 translator.translate()를 호출한다.
 * Given: tossPaymentClient가 TossPaymentConfirmErrorRes 반환 stub
 * When: approve() 호출
 * Then: translateor.translate() 호출해야함
 *
 * DisplayName: tossPaymentClient가 timeout예외 반환 시 translator.translateTimeout()을 호출한다.
 * Given: tossPaymentClient가 ResourceAccessException 반환 stub
 * When: approve() 호출
 * Then: translateor.translateTimeout() 호출해야함
 *
 * DisplayName: tossPaymentClient가 2xx/4xx/5xx/timeout 계열 예외가 아닌 알 수 없는 예외 반환 시 translator.translateUnknown()을 호출한다.
 * Given: tossPaymentClient가 RuntimeException 반환 stub
 * When: approve() 호출
 * Then: translateor.translateUnknown() 호출해야함
 *
 * DisplayName: tossPaymentClient 호출 시 요청 uri는 tossPaymentProperties.uri()와 같다.
 * Given: tossPaymentProperties.uri() 특정 Uri 반환 stub
 * When: approve() 호출
 * Then: tossPaymentClient.post().uri() 확인
 *
 * DisplayName: tossPaymentClient 호출 시 요청 헤더는 Authorization, Content-Type, Idempotency-Key를 key로 하는 필드에 properties의 값이 정상적으로 들어간다.
 * Given: secret, contentType, orderCode 준비
 * When: approve() 호출
 * Then: Authorization, Content-Type, Idempotency-Key 헤더 Assertion
 *
 * DisplayName: tossPaymentClient 호출 시 요청 헤더 중 Authorization을 키로하는 필드의 값은 Base64 암호화된다.
 * Given: tossPaymentProperties.secret()이 "test_secret"을 반환한다.
 * When: approve(paymentPayload)를 호출한다.
 * Then: Authorization 헤더 값은 "Basic " + Base64("test_secret:") 이다.
 *
 * DisplayName: tossPaymentClient 호출 시 요청 body는 PaymentPayload이다.
 * Given: paymentPayload가 준비되어 있다.
 * When: approve(paymentPayload)를 호출한다.
 * Then: tossPaymentClient 요청 body에 paymentPayload가 전달된다.
 */

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
