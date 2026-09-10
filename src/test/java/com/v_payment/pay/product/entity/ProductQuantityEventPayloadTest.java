package com.v_payment.pay.product.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.v_payment.pay.payment.entity.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProductQuantityEventPayloadTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("JSON 문자열의 상품 ID key를 Long key로 역직렬화한다")
    @Test
    void deserializeRequestedQuantitiesWithLongKey() throws Exception {
        // given
        String json = """
                {
                  "orderCode": "ORDER-001",
                  "paymentMethod": "CARD",
                  "requestedQuantities": {
                    "1": 2,
                    "601": 3
                  }
                }
                """;

        // when
        ProductQuantityEventPayload payload = objectMapper.readValue(json, ProductQuantityEventPayload.class);

        // then
        assertThat(payload.getOrderCode()).isEqualTo("ORDER-001");
        assertThat(payload.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(payload.getRequestedQuantities()).containsExactlyInAnyOrderEntriesOf(Map.of(
                1L, 2,
                601L, 3
        ));
        assertThat(payload.getRequestedQuantities().keySet())
                .allSatisfy(productId -> assertThat(productId).isInstanceOf(Long.class));
    }

    @DisplayName("requestedQuantities를 JSON object 형태로 직렬화한다")
    @Test
    void serializeRequestedQuantities() throws Exception {
        // given
        ProductQuantityEventPayload payload = ProductQuantityEventPayload.of(
                "ORDER-001",
                PaymentMethod.CARD,
                Map.of(1L, 2, 601L, 3)
        );

        // when
        String json = objectMapper.writeValueAsString(payload);

        // then
        assertThat(json).contains("\"orderCode\":\"ORDER-001\"");
        assertThat(json).contains("\"paymentMethod\":\"CARD\"");
        assertThat(json).contains("\"requestedQuantities\"");
        assertThat(json).contains("\"1\":2");
        assertThat(json).contains("\"601\":3");
    }
}
