package com.v_payment.pay.order.service;

import com.v_payment.pay.order.domain.entity.Order;
import com.v_payment.pay.order.domain.entity.OrderStatus;
import com.v_payment.pay.order.repository.OrderRepository;
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
class OrderExpirationServiceTest {
    @Autowired
    OrderExpirationService orderExpirationService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    Clock clock;

    @DisplayName("old created orders are expired")
    @Test
    void expireCreatedOrders() {
        Order oldOrder = orderRepository.save(Order.create(
                "old-order",
                LocalDateTime.now(clock).minusMinutes(20)
        ));
        Order newOrder = orderRepository.save(Order.create(
                "new-order",
                LocalDateTime.now(clock)
        ));

        int expiredCount = orderExpirationService.expireCreatedOrders();
        entityManager.flush();
        entityManager.clear();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(orderRepository.findById(oldOrder.getOrderId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.EXPIRED);
        assertThat(orderRepository.findById(newOrder.getOrderId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CREATED);
    }
}
