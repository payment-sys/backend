package com.v_payment.pay.order.application;

import com.v_payment.pay.order.entrypoint.dto.req.OrderCreateReq;
import com.v_payment.pay.order.entrypoint.dto.res.OrderCreateRes;
import com.v_payment.pay.order.domain.order.RequestedOrder;
import com.v_payment.pay.order.domain.order.Order;
import com.v_payment.pay.order.domain.outbox.QuantityChangeOutbox;
import com.v_payment.pay.order.infrastructure.persistence.repository.OrderRepository;
import com.v_payment.pay.order.infrastructure.persistence.repository.QuantityChangeOutboxRepository;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderUseCase {
    private final Clock clock;
    private final OrderRepository orderRepository;
    private final QuantityChangeOutboxRepository quantityChangeOutboxRepository;
    private final ApplicationEventPublisher eventPublisher;

    @WithSpan("order.OrderUseCase.create")
    @Transactional
    public OrderCreateRes create(OrderCreateReq req) {
        String orderCode = UUID.randomUUID().toString();
        RequestedOrder requestedOrder = RequestedOrder.of(orderCode, req.items());
        Order order = Order.create(orderCode, req.paymentMethod(), LocalDateTime.now(clock));
        orderRepository.save(order);
        QuantityChangeOutbox quantityChangeOutbox = QuantityChangeOutbox.create(requestedOrder, clock);
        quantityChangeOutboxRepository.save(quantityChangeOutbox);
        eventPublisher.publishEvent(quantityChangeOutbox);
        return OrderCreateRes.from(orderCode);
    }
}
