package com.v_payment.pay.order.application;

import com.v_payment.pay.global.exception.BusinessException;
import com.v_payment.pay.order.entrypoint.dto.req.OrderCreateReq;
import com.v_payment.pay.order.entrypoint.dto.res.OrderCreateRes;
import com.v_payment.pay.order.domain.orderitem.OrderItemSources;
import com.v_payment.pay.order.domain.order.RequestedOrder;
import com.v_payment.pay.order.domain.order.Order;
import com.v_payment.pay.order.domain.outbox.QuantityChangeOutbox;
import com.v_payment.pay.order.exception.OrderException;
import com.v_payment.pay.order.infrastructure.persistence.repository.OrderRepository;
import com.v_payment.pay.order.infrastructure.persistence.repository.QuantityChangeOutboxRepository;
import com.v_payment.pay.payment.domain.entity.PaymentMethod;
import com.v_payment.pay.product.domain.ProductBasicInfo;
import com.v_payment.pay.product.service.ProductManager;
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
    private final ProductManager productManager;
    private final ApplicationEventPublisher eventPublisher;

    @WithSpan("order.OrderUseCase.create")
    @Transactional
    public OrderCreateRes create(OrderCreateReq req) {
        String orderCode = UUID.randomUUID().toString();
        RequestedOrder requestedOrder = RequestedOrder.of(orderCode, req.items());
        createOrder(orderCode, req.paymentMethod(), requestedOrder);
        QuantityChangeOutbox quantityChangeOutbox = QuantityChangeOutbox.create(requestedOrder, clock);
        quantityChangeOutboxRepository.save(quantityChangeOutbox);
        eventPublisher.publishEvent(quantityChangeOutbox);
        return OrderCreateRes.from(orderCode);
    }

    private void createOrder(String orderCode, PaymentMethod paymentMethod, RequestedOrder requestedOrder) {
        Order order = Order.create(orderCode, paymentMethod, LocalDateTime.now(clock));
        OrderItemSources orderItemSources = createOrderItemSources(requestedOrder);
        order.addItems(orderItemSources);
        orderRepository.save(order);
    }

    private OrderItemSources createOrderItemSources(RequestedOrder requestedOrder) {
        List<ProductBasicInfo> productBasicInfos = productManager.findProductBasicInfos(requestedOrder.getProductIds());
        OrderItemSources orderItemSources = OrderItemSources.of(productBasicInfos, requestedOrder);

        if (!requestedOrder.hasAllProducts(orderItemSources.getOrderItemCount()))
            throw new BusinessException(OrderException.ORDER_ITEM_NOT_FOUND);

        return orderItemSources;
    }
}
