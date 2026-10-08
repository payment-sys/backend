package com.v_payment.pay.order.service;

import com.v_payment.pay.global.exception.BusinessException;
import com.v_payment.pay.order.entrypoint.dto.req.OrderCreateReq;
import com.v_payment.pay.order.entrypoint.dto.res.OrderCreateRes;
import com.v_payment.pay.order.domain.OrderPaymentCreateSource;
import com.v_payment.pay.order.domain.QuantityChangeSummaries;
import com.v_payment.pay.order.domain.orderitem.OrderItemSources;
import com.v_payment.pay.order.domain.ReqQuantities;
import com.v_payment.pay.order.domain.order.Order;
import com.v_payment.pay.order.domain.order.OrderStatus;
import com.v_payment.pay.order.domain.outbox.QuantityChangeOutbox;
import com.v_payment.pay.order.exception.OrderException;
import com.v_payment.pay.order.infra.kafka.dto.QuantityChangeSummaryMessage;
import com.v_payment.pay.order.infra.kafka.dto.QuantityChangeSummaryStatus;
import com.v_payment.pay.order.repository.OrderRepository;
import com.v_payment.pay.order.repository.QuantityChangeOutboxRepository;
import com.v_payment.pay.payment.domain.entity.PaymentMethod;
import com.v_payment.pay.payment.service.PaymentManager;
import com.v_payment.pay.product.domain.ProductBasicInfo;
import com.v_payment.pay.product.service.ProductManager;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final Clock clock;
    private final OrderRepository orderRepository;
    private final QuantityChangeOutboxRepository quantityChangeOutboxRepository;
    private final ProductManager productManager;
    private final PaymentManager paymentManager;
    private final ApplicationEventPublisher eventPublisher;

    @WithSpan("order.OrderService.create")
    @Transactional
    public OrderCreateRes create(OrderCreateReq req) {
        String orderCode = UUID.randomUUID().toString();
        ReqQuantities reqQuantities = ReqQuantities.of(orderCode, req.items());
        createOrder(orderCode, req.paymentMethod(), reqQuantities);
        QuantityChangeOutbox quantityChangeOutbox = QuantityChangeOutbox.of(reqQuantities, clock);
        quantityChangeOutboxRepository.save(quantityChangeOutbox);
        eventPublisher.publishEvent(quantityChangeOutbox);
        return OrderCreateRes.from(orderCode);
    }

    @Transactional
    public void finalizeOrderSummaryBatch(List<QuantityChangeSummaryMessage> quantityChangeSummaryMessages) {
        QuantityChangeSummaries summaries = QuantityChangeSummaries.create(quantityChangeSummaryMessages);
        if (summaries.isEmpty()) return;

        List<Order> orders = orderRepository.findAllByOrderCodeInAndStatus(
                summaries.getMessages().stream()
                        .map(QuantityChangeSummaryMessage::orderCode)
                        .toList(),
                OrderStatus.CREATED
        );
        Map<String, Order> ordersByCode = orders.stream()
                .collect(Collectors.toMap(Order::getOrderCode, Function.identity()));

        List<OrderPaymentCreateSource> paymentCreateSources = summaries.getMessages().stream()
                .map(message -> applyQuantityChangeSummary(message, ordersByCode.get(message.orderCode())))
                .filter(Objects::nonNull)
                .toList();

        paymentManager.createPendingPayments(getList(paymentCreateSources));
    }

    private OrderPaymentCreateSource applyQuantityChangeSummary(QuantityChangeSummaryMessage message, Order order) {
        if (order == null || !order.isCreated()) {
            return null;
        }

        if (message.status() == QuantityChangeSummaryStatus.FAILED) {
            order.applyQuantityChangeResult(
                    OrderStatus.LACK_QUANTITY,
                    message.successProductIds(),
                    message.failedProductIds()
            );
            publishCompensationOutbox(order, message.successProductIds());
            return null;
        }

        order.applyQuantityChangeResult(
                OrderStatus.ORDER_SUCCESS,
                message.successProductIds(),
                message.failedProductIds()
        );
        return new OrderPaymentCreateSource(order.getOrderCode(), order.getTotalAmount(), order.getPaymentMethod());
    }

    private void publishCompensationOutbox(Order order, List<Long> successProductIds) {
        if (successProductIds == null || successProductIds.isEmpty()) {
            return;
        }

        Set<Long> productIds = Set.copyOf(successProductIds);
        Map<Long, Integer> quantitiesByProductId = order.getOrderItems().stream()
                .filter(orderItem -> productIds.contains(orderItem.getOrderItemInfo().getProductId()))
                .collect(Collectors.toMap(
                        orderItem -> orderItem.getOrderItemInfo().getProductId(),
                        orderItem -> orderItem.getOrderItemInfo().getQuantity(),
                        Integer::sum
                ));
        if (quantitiesByProductId.isEmpty()) {
            return;
        }

        QuantityChangeOutbox compensationOutbox = QuantityChangeOutbox.compensate(
                order.getOrderCode(),
                quantitiesByProductId,
                clock
        );
        quantityChangeOutboxRepository.save(compensationOutbox);
        eventPublisher.publishEvent(compensationOutbox);
    }

    private static @NonNull List<PaymentManager.PendingPaymentCreateRequest> getList(List<OrderPaymentCreateSource> paymentCreateSources) {
        return paymentCreateSources.stream()
                .map(source -> new PaymentManager.PendingPaymentCreateRequest(
                        source.orderCode(),
                        source.amount(),
                        source.paymentMethod()
                ))
                .toList();
    }

    private void createOrder(String orderCode, PaymentMethod paymentMethod, ReqQuantities reqQuantities) {
        Order order = Order.create(orderCode, paymentMethod, LocalDateTime.now(clock));
        OrderItemSources orderItemSources = createOrderItemSources(reqQuantities);
        order.addItems(orderItemSources);
        orderRepository.save(order);
    }

    private OrderItemSources createOrderItemSources(ReqQuantities reqQuantities) {
        List<ProductBasicInfo> productBasicInfos = productManager.findProductBasicInfos(reqQuantities.getProductIds());
        OrderItemSources orderItemSources = OrderItemSources.of(productBasicInfos, reqQuantities);

        if (!reqQuantities.hasAllProducts(orderItemSources.getOrderItemCount()))
            throw new BusinessException(OrderException.ORDER_ITEM_NOT_FOUND);

        return orderItemSources;
    }
}
