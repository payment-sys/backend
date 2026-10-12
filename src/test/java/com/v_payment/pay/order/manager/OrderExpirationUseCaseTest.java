package com.v_payment.pay.order.manager;

import com.v_payment.pay.order.application.OrderExpirationUseCase;
import com.v_payment.pay.order.domain.order.Order;
import com.v_payment.pay.order.domain.order.OrderItemInfo;
import com.v_payment.pay.order.domain.order.OrderItemStatus;
import com.v_payment.pay.order.domain.order.OrderStatus;
import com.v_payment.pay.order.domain.outbox.QuantityChangeOutbox;
import com.v_payment.pay.order.domain.outbox.QuantityChangeOutboxType;
import com.v_payment.pay.order.infrastructure.persistence.repository.OrderJdbcRepository;
import com.v_payment.pay.order.infrastructure.persistence.repository.OrderRepository;
import com.v_payment.pay.order.infrastructure.persistence.repository.QuantityChangeOutboxRepository;
import com.v_payment.pay.payment.domain.entity.Payment;
import com.v_payment.pay.payment.domain.entity.PaymentMethod;
import com.v_payment.pay.payment.repository.PaymentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@Transactional
class OrderExpirationUseCaseTest {
    @Autowired
    OrderExpirationUseCase orderExpirationUseCase;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderJdbcRepository orderJdbcRepository;

    @Autowired
    QuantityChangeOutboxRepository quantityChangeOutboxRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    Clock clock;

    @DisplayName("old created orders are expired")
    @Test
    void expireOrders() {
        Order oldOrder = orderRepository.save(Order.create(
                "old-order",
                LocalDateTime.now(clock).minusMinutes(20)
        ));
        Order newOrder = orderRepository.save(Order.create(
                "new-order",
                LocalDateTime.now(clock)
        ));

        int expiredCount = orderExpirationUseCase.expireOrders();
        entityManager.flush();
        entityManager.clear();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(orderRepository.findById(oldOrder.getOrderId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.EXPIRED);
        assertThat(orderRepository.findById(newOrder.getOrderId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CREATED);
    }

    @DisplayName("old order success orders without active payment are expired and compensated")
    @Test
    void expireOrderSuccessOrders() {
        String orderCode = createOrderSuccessOrder("order-success-without-payment");

        int expiredCount = orderExpirationUseCase.expireOrders();
        entityManager.flush();
        entityManager.clear();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(orderRepository.findByOrderCode(orderCode).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.EXPIRED);

        List<QuantityChangeOutbox> outboxes = quantityChangeOutboxRepository.findAll();
        assertThat(outboxes).hasSize(1);
        QuantityChangeOutbox outbox = outboxes.get(0);
        assertThat(outbox.getType()).isEqualTo(QuantityChangeOutboxType.COMPENSATE);
        assertThat(outbox.getRequestedOrder().getQuantityMap()).containsEntry(1L, 2);
    }

    @DisplayName("old order success orders with ready payment are not expired")
    @Test
    void doNotExpireOrderSuccessOrdersWithReadyPayment() {
        String orderCode = createOrderSuccessOrder("order-success-with-ready-payment");
        paymentRepository.save(Payment.createReady(
                orderCode,
                orderCode,
                20_000L,
                PaymentMethod.CARD,
                clock
        ));

        int expiredCount = orderExpirationUseCase.expireOrders();
        entityManager.flush();
        entityManager.clear();

        assertThat(expiredCount).isZero();
        assertThat(orderRepository.findByOrderCode(orderCode).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.ORDER_SUCCESS);
        assertThat(quantityChangeOutboxRepository.findAll()).isEmpty();
    }

    private String createOrderSuccessOrder(String orderCode) {
        orderRepository.save(Order.create(
                orderCode,
                LocalDateTime.now(clock).minusMinutes(20)
        ));
        entityManager.flush();
        entityManager.clear();

        List<OrderItemInfo> orderItems = List.of(
                OrderItemInfo.create(1L, "product", 10_000L, 2, OrderItemStatus.CHANGED)
        );
        orderJdbcRepository.updateSummaryBatch(List.of(OrderJdbcRepository.OrderSummaryUpdate.of(
                orderCode,
                OrderStatus.ORDER_SUCCESS,
                orderItems,
                20_000L
        )));
        entityManager.flush();
        entityManager.clear();
        return orderCode;
    }
}
