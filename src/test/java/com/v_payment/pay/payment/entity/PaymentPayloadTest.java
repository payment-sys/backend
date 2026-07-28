package com.v_payment.pay.payment.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class PaymentPayloadTest {

    @DisplayName("Json 직렬화 시 orderCode는 orderId가 돼야 한다.")
    @Test
    void paymentPayloadTest1() {
        //given
        ObjectMapper objectMapper = new ObjectMapper();

        String orderCode = "orderCode";
        String paymentKey = "orderId";
        Long amount = 1L;

        PaymentPayload paymentPayload = PaymentPayload.create(orderCode, paymentKey, amount);

        //when
        String json = objectMapper.writeValueAsString(paymentPayload);

        //then
        Assertions.assertThat(json).contains("orderId");
    }
}