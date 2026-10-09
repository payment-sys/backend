package com.v_payment.pay.order.manager;

import com.v_payment.pay.order.domain.order.Order;
import com.v_payment.pay.order.domain.order.OrderStatus;
import com.v_payment.pay.order.infrastructure.persistence.repository.OrderRepository;
import jakarta.persistence.EntityManager;
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
class OrderManagerTest {
    @Autowired
    OrderManager orderManager;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    Clock clock;

    @Autowired
    EntityManager entityManager;

    @DisplayName("inventory secured order can be marked paid")
    @Test
    void markPaidFromOrderSuccess() {
        String orderCode = createOrderSuccessOrder("paid-order");

        boolean updated = orderManager.markPaid(orderCode);

        assertThat(updated).isTrue();
        assertThat(findStatus(orderCode)).isEqualTo(OrderStatus.PAID);
    }

    @DisplayName("created order can be marked lack quantity when inventory change fails")
    @Test
    void markLackQuantityFromCreated() {
        String orderCode = createCreatedOrder("lack-order");

        boolean updated = orderManager.markLackQuantity(orderCode);

        assertThat(updated).isTrue();
        assertThat(findStatus(orderCode)).isEqualTo(OrderStatus.LACK_QUANTITY);
    }

    @DisplayName("created order can be marked expired by scheduler")
    @Test
    void markExpiredFromCreated() {
        String orderCode = createCreatedOrder("expired-order");

        boolean updated = orderManager.markExpired(orderCode);

        assertThat(updated).isTrue();
        assertThat(findStatus(orderCode)).isEqualTo(OrderStatus.EXPIRED);
    }

    private String createOrderSuccessOrder(String orderCode) {
        createCreatedOrder(orderCode);
        orderRepository.markStatus(orderCode, OrderStatus.CREATED, OrderStatus.ORDER_SUCCESS);
        entityManager.flush();
        entityManager.clear();
        return orderCode;
    }

    private String createCreatedOrder(String orderCode) {
        orderRepository.save(Order.create(orderCode, LocalDateTime.now(clock)));
        entityManager.flush();
        entityManager.clear();
        return orderCode;
    }

    private OrderStatus findStatus(String orderCode) {
        return orderRepository.findByOrderCode(orderCode)
                .orElseThrow()
                .getStatus();
    }
}
