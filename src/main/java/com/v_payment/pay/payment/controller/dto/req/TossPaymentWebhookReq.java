package com.v_payment.pay.payment.controller.dto.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * unit test
 *
 * DisplayName: orderId 필드를 orderCode로 파싱할 수 있다.
 * Given : orderId 필드를 가진 Json String
 * When : @JsonTest를 통한 파싱
 * Then : orderCode에 String 형식의 주문 코드 존재
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentWebhookReq(
        String eventType,
        LocalDateTime createdAt,
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            @JsonAlias("orderId")
            String orderCode,
            String paymentKey,
            String status,
            Long totalAmount,
            OffsetDateTime approvedAt,
            Receipt receipt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Receipt(
            String url
    ) {
    }
}
