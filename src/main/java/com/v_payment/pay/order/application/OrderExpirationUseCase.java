package com.v_payment.pay.order.application;

import com.v_payment.pay.order.config.OrderExpirationProperties;
import com.v_payment.pay.order.domain.order.Order;
import com.v_payment.pay.order.domain.order.OrderExpirePolicy;
import com.v_payment.pay.order.domain.order.OrderStatus;
import com.v_payment.pay.order.domain.outbox.QuantityChangeOutbox;
import com.v_payment.pay.order.infrastructure.persistence.repository.OrderRepository;
import com.v_payment.pay.order.infrastructure.persistence.repository.QuantityChangeOutboxJdbcRepository;
import com.v_payment.pay.order.manager.OrderManager;
import com.v_payment.pay.payment.domain.entity.PaymentStatus;
import com.v_payment.pay.payment.repository.PaymentRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j(topic = "SCHEDULER_LOGGER")
@Service
@RequiredArgsConstructor
public class OrderExpirationUseCase {
    private static final List<PaymentStatus> PAYMENT_IN_PROGRESS_STATUSES = List.of(
            PaymentStatus.READY,
            PaymentStatus.IN_PROGRESS,
            PaymentStatus.DONE
    );

    private final Clock clock;
    private final OrderExpirationProperties orderExpirationProperties;
    private final OrderRepository orderRepository;
    private final OrderManager orderManager;
    private final PaymentRepository paymentRepository;
    private final QuantityChangeOutboxJdbcRepository quantityChangeOutboxJdbcRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    @Transactional
    public int expireOrders() {
        LocalDateTime orderedBefore = LocalDateTime.now(clock)
                .minusSeconds(orderExpirationProperties.expireAfterSeconds());
        int expiredCreatedCount = expireCreatedOrders(orderedBefore);
        int expiredOrderSuccessCount = expireOrderSuccessOrders(orderedBefore);
        return expiredCreatedCount + expiredOrderSuccessCount;
    }

    private int expireCreatedOrders(LocalDateTime orderedBefore) {
        List<String> orderCodes = orderRepository.findExpirableCreatedOrderCodes(OrderStatus.CREATED, orderedBefore,
                PageRequest.of(0, orderExpirationProperties.batchSize()));

        if (orderCodes.isEmpty()) return 0;

        boolean updated = orderManager.markExpired(orderCodes);
        if (!updated) {
            log.error("주문 만료 상태 업데이트를 실패했습니다. orderCodes={}", orderCodes);
            throw new IllegalStateException("주문 만료 상태 업데이트를 실패했습니다.");
        }
        return orderCodes.size();
    }

    private int expireOrderSuccessOrders(LocalDateTime orderedBefore) {
        List<Order> candidates = orderRepository.findExpirableOrdersForUpdate(
                OrderStatus.ORDER_SUCCESS,
                orderedBefore,
                PageRequest.of(0, orderExpirationProperties.batchSize())
        );
        if (candidates.isEmpty()) return 0;

        OrderExpirePolicy orderExpirePolicy = new OrderExpirePolicy();
        List<String> candidateOrderCodes = candidates.stream()
                .map(Order::getOrderCode)
                .toList();
        List<String> paymentInProgressOrderCodes = paymentRepository.findOrderCodesByPaymentStatuses(
                candidateOrderCodes,
                PAYMENT_IN_PROGRESS_STATUSES
        );
        List<Order> expirableOrders = orderExpirePolicy.excludePaymentInProgressOrders(candidates, paymentInProgressOrderCodes);
        if (expirableOrders.isEmpty()) return 0;

        List<QuantityChangeOutbox> compensationOutboxes = createCompensationOutboxes(expirableOrders, orderExpirePolicy);
        List<String> expirableOrderCodes = expirableOrders.stream()
                .map(Order::getOrderCode)
                .toList();
        boolean updated = orderManager.markOrderSuccessExpired(expirableOrderCodes);
        if (!updated) {
            log.error("주문 만료 상태 업데이트를 실패했습니다. orderCodes={}", expirableOrderCodes);
            throw new IllegalStateException("주문 만료 상태 업데이트를 실패했습니다.");
        }

        clearPersistenceContext();
        sendCompensationOutboxes(compensationOutboxes);
        return expirableOrders.size();
    }

    private void clearPersistenceContext() {
        entityManager.clear();
    }

    private List<QuantityChangeOutbox> createCompensationOutboxes(List<Order> orders, OrderExpirePolicy orderExpirePolicy) {
        return orders.stream()
                .map(order -> createCompensationOutbox(order, orderExpirePolicy.successQuantities(order)))
                .filter(outbox -> outbox != null)
                .toList();
    }

    private QuantityChangeOutbox createCompensationOutbox(Order order, Map<Long, Integer> successQuantities) {
        if (successQuantities == null || successQuantities.isEmpty()) return null;
        return QuantityChangeOutbox.compensate(order.getOrderCode(), successQuantities, clock);
    }

    private void sendCompensationOutboxes(List<QuantityChangeOutbox> compensationOutboxes) {
        if (compensationOutboxes.isEmpty()) return;
        List<QuantityChangeOutbox> insertedOutboxes = quantityChangeOutboxJdbcRepository.createOutboxBatch(compensationOutboxes);
        insertedOutboxes.forEach(eventPublisher::publishEvent);
    }
}
