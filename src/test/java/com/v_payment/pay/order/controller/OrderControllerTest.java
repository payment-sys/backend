package com.v_payment.pay.order.controller;

import com.v_payment.pay.order.controller.dto.req.OrderCreateReq;
import com.v_payment.pay.order.controller.dto.res.OrderCreateRes;
import com.v_payment.pay.order.service.OrderService;
import com.v_payment.pay.global.meter.ApiRequestConcurrencyFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = OrderController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = ApiRequestConcurrencyFilter.class
        )
)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @DisplayName("주문 생성 요청이 유효하면 주문 코드를 응답한다")
    @Test
    void createOrder() throws Exception {
        // given
        String req = """
                {
                    "paymentMethod": "CARD",
                    "items": [
                        {
                            "productId": 1,
                            "quantity": 2
                        }
                    ]
                }
                """;
        given(orderService.create(any(OrderCreateReq.class)))
                .willReturn(new OrderCreateRes("ORDER-001"));

        // when & then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCode").value("ORDER-001"));

        verify(orderService).create(any(OrderCreateReq.class));
    }

    @DisplayName("결제 수단이 없으면 주문 생성 요청이 실패한다")
    @Test
    void createOrderWithoutPaymentMethod() throws Exception {
        // given
        String req = """
                {
                    "items": [
                        {
                            "productId": 1,
                            "quantity": 2
                        }
                    ]
                }
                """;

        // when & then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("주문 상품 목록이 비어 있으면 주문 생성 요청이 실패한다")
    @Test
    void createOrderWithEmptyItems() throws Exception {
        // given
        String req = """
                {
                    "paymentMethod": "CARD",
                    "items": []
                }
                """;

        // when & then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("주문 상품 수량이 양수가 아니면 주문 생성 요청이 실패한다")
    @Test
    void createOrderWithInvalidQuantity() throws Exception {
        // given
        String req = """
                {
                    "paymentMethod": "CARD",
                    "items": [
                        {
                            "productId": 1,
                            "quantity": 0
                        }
                    ]
                }
                """;

        // when & then
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isBadRequest());
    }
}
