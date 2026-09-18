package com.v_payment.pay.payment.service;

import com.v_payment.pay.order.domain.entity.Order;
import com.v_payment.pay.order.repository.OrderRepository;
import com.v_payment.pay.payment.controller.dto.req.PaymentRegenerateReq;
import com.v_payment.pay.payment.controller.dto.res.PaymentRegenerateRes;
import com.v_payment.pay.payment.domain.entity.Payment;
import com.v_payment.pay.payment.domain.entity.PaymentMethod;
import com.v_payment.pay.payment.domain.entity.PaymentStatus;
import com.v_payment.pay.payment.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@Transactional
class PaymentRegenerationServiceTest {
    @Autowired
    PaymentRegenerationService paymentRegenerationService;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    Clock clock;

    @DisplayName("ready payment is expired before regenerated payment is created")
    @Test
    void regeneratePaymentExpiresReadyPayment() {
        orderRepository.save(Order.create("order-code", LocalDateTime.now(clock)));
        Payment readyPayment = paymentRepository.save(Payment.createReady(
                "order-code",
                "old-idempotency-key",
                1000L,
                PaymentMethod.CARD,
                clock
        ));

        PaymentRegenerateRes response = paymentRegenerationService.regeneratePayment(
                new PaymentRegenerateReq("order-code")
        );

        assertThat(response.orderCode()).isEqualTo("order-code");
        assertThat(response.idempotencyKey()).isNotEqualTo("old-idempotency-key");
        assertThat(response.status()).isEqualTo(PaymentStatus.READY);

        assertThat(paymentRepository.findById(readyPayment.getId()).orElseThrow().getPaymentStatus())
                .isEqualTo(PaymentStatus.EXPIRED);
        assertThat(paymentRepository.findAllByOrderCode("order-code"))
                .extracting(Payment::getPaymentStatus)
                .containsExactlyInAnyOrder(PaymentStatus.EXPIRED, PaymentStatus.READY);
    }
}
