package com.v_payment.pay.order.application;

import com.v_payment.pay.order.domain.PaymentCreateSource;
import com.v_payment.pay.order.domain.QuantityChangeSummaries;
import com.v_payment.pay.order.domain.order.Order;
import com.v_payment.pay.order.domain.order.OrderStatus;
import com.v_payment.pay.order.domain.outbox.QuantityChangeOutbox;
import com.v_payment.pay.order.infrastructure.kafka.dto.QuantityChangeSummaryMessage;
import com.v_payment.pay.order.infrastructure.persistence.repository.OrderJdbcRepository;
import com.v_payment.pay.order.infrastructure.persistence.repository.OrderRepository;
import com.v_payment.pay.order.infrastructure.persistence.repository.QuantityChangeOutboxJdbcRepository;
import com.v_payment.pay.payment.service.PaymentManager;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuantityChangeSummaryUseCase {
    private final Clock clock;
    private final OrderRepository orderRepository;
    private final OrderJdbcRepository orderJdbcRepository;
    private final QuantityChangeOutboxJdbcRepository quantityChangeOutboxJdbcRepository;
    private final PaymentManager paymentManager;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    @Transactional
    public void finalizeOrderSummaryBatch(List<QuantityChangeSummaryMessage> messages) {
        QuantityChangeSummaries summaries = QuantityChangeSummaries.create(messages);
        if (summaries.isEmpty()) return;

        Map<String, Order> orders = findOrders(summaries);

        List<QuantityChangeSummaryMessage> successMessages = filterProcessableMessages(summaries.getSuccessMessages(), orders);
        List<QuantityChangeSummaryMessage> failedMessages = filterProcessableMessages(summaries.getFailedMessages(), orders);

        orderJdbcRepository.updateSummaryBatch(toSummaryUpdates(successMessages, OrderStatus.ORDER_SUCCESS));
        List<PaymentCreateSource> paymentCreateSources = createPaymentSources(successMessages, orders);

        orderJdbcRepository.updateSummaryBatch(toSummaryUpdates(failedMessages, OrderStatus.LACK_QUANTITY));
        List<QuantityChangeOutbox> compensationOutboxes = createCompensationOutboxes(failedMessages, orders);

        clearPersistenceContext();

        createPayments(paymentCreateSources);
        sendCompensation(compensationOutboxes);
    }

    private List<QuantityChangeSummaryMessage> filterProcessableMessages(
            List<QuantityChangeSummaryMessage> messages,
            Map<String, Order> orders
    ) {
        return messages.stream()
                .filter(msg -> orders.containsKey(msg.orderCode()))
                .toList();
    }

    private List<PaymentCreateSource> createPaymentSources(List<QuantityChangeSummaryMessage> successMessages, Map<String, Order> orders) {
        return successMessages.stream()
                .map(msg -> orders.get(msg.orderCode()))
                .map(order -> PaymentCreateSource.create(order.getOrderCode(), order.getTotalAmount(),
                        order.getPaymentMethod()))
                .toList();
    }

    private List<QuantityChangeOutbox> createCompensationOutboxes(List<QuantityChangeSummaryMessage> failedMsgs, Map<String, Order> orders) {
        return failedMsgs.stream()
                .map(msg -> createCompensationOutbox(orders.get(msg.orderCode()), msg.successQuantities()))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<OrderJdbcRepository.OrderSummaryUpdate> toSummaryUpdates(List<QuantityChangeSummaryMessage> messages,
                                                                          OrderStatus targetStatus) {
        return messages.stream()
                .map(msg -> OrderJdbcRepository.OrderSummaryUpdate.of(msg.orderCode(),
                        targetStatus, msg.successProductIds(), msg.failedProductIds()))
                .toList();
    }

    private void createPayments(List<PaymentCreateSource> paymentCreateSources) {
        paymentManager.createPendingPayments(paymentCreateSources);
    }

    private void sendCompensation(List<QuantityChangeOutbox> compensationOutboxes) {
        if (compensationOutboxes.isEmpty()) return;
        List<QuantityChangeOutbox> insertedOutboxes = quantityChangeOutboxJdbcRepository.createOutboxBatch(compensationOutboxes);
        insertedOutboxes.forEach(eventPublisher::publishEvent);
    }

    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }

    private QuantityChangeOutbox createCompensationOutbox(Order order, Map<Long, Integer> successQuantities) {
        if (successQuantities == null || successQuantities.isEmpty()) return null;
        return QuantityChangeOutbox.compensate(order.getOrderCode(), successQuantities, clock);
    }

    private Map<String, Order> findOrders(QuantityChangeSummaries summaries) {
        return orderRepository.findAllByOrderCodeCreated(summaries.getOrderCodes(), OrderStatus.CREATED)
                .stream().collect(Collectors.toMap(Order::getOrderCode, Function.identity()));
    }
}
