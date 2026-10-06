package com.v_payment.pay.order.service;

import com.v_payment.pay.global.exception.BusinessException;
import com.v_payment.pay.order.controller.dto.req.OrderCreateReq;
import com.v_payment.pay.order.controller.dto.res.OrderCreateRes;
import com.v_payment.pay.order.domain.OrderPaymentCreateSource;
import com.v_payment.pay.order.domain.QuantityChangeResultPlan;
import com.v_payment.pay.order.domain.QuantityChangeSummaries;
import com.v_payment.pay.order.domain.orderitem.OrderItemSources;
import com.v_payment.pay.order.domain.orderitem.OrderItemStatus;
import com.v_payment.pay.order.domain.ReqQuantities;
import com.v_payment.pay.order.domain.order.Order;
import com.v_payment.pay.order.domain.order.OrderStatus;
import com.v_payment.pay.order.domain.outbox.QuantityChangeOutbox;
import com.v_payment.pay.order.exception.OrderException;
import com.v_payment.pay.order.infra.kafka.dto.QuantityChangeResultMessage;
import com.v_payment.pay.order.infra.kafka.dto.QuantityChangeSummaryMessage;
import com.v_payment.pay.order.repository.OrderItemRepository;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final Clock clock;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
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
    public void finalizeOrderBatch(List<QuantityChangeResultMessage> quantityChangeResultMessages) {
        QuantityChangeResultPlan plan = QuantityChangeResultPlan.create(quantityChangeResultMessages);
        orderItemRepository.updateStatusByQuantityChangeResults(plan.getMessages(), OrderItemStatus.PROCESSING,
                OrderItemStatus.CHANGED, OrderItemStatus.FAILED);
        List<String> failedOrderCodes = orderItemRepository.findOrderCodesByItemStatus(plan.getOrderCodes(),
                OrderItemStatus.FAILED);
        if (!failedOrderCodes.isEmpty()) orderRepository.markStatusByOrderCodes(failedOrderCodes, OrderStatus.CREATED,
                OrderStatus.LACK_QUANTITY);
        List<OrderPaymentCreateSource> paymentCreateSources = orderRepository.findPaymentCreateSourcesForCompletedOrders(
                plan.getOrderCodes(), OrderStatus.CREATED, OrderItemStatus.CHANGED);
        List<String> completedOrderCodes = plan.getCompletedOrderCodes(paymentCreateSources);
        if (completedOrderCodes.isEmpty()) return;
        orderRepository.markStatusByOrderCodes(completedOrderCodes, OrderStatus.CREATED, OrderStatus.ORDER_SUCCESS);
        paymentManager.createPendingPayments(getList(paymentCreateSources));
    }

    @Transactional
    public void finalizeOrderSummaryBatch(List<QuantityChangeSummaryMessage> quantityChangeSummaryMessages) {
        QuantityChangeSummaries summaries = QuantityChangeSummaries.create(quantityChangeSummaryMessages);
        if (summaries.isEmpty()) return;
        if (summaries.hasFailedOrders()) orderRepository.markStatusByOrderCodes(summaries.getFailedOrderCodes(),
                    OrderStatus.CREATED, OrderStatus.LACK_QUANTITY);
        if (!summaries.hasSuccessOrders()) return;
        List<OrderPaymentCreateSource> paymentCreateSources =
                orderRepository.findPaymentCreateSourcesByOrderCodes(summaries.getSuccessOrderCodes(), OrderStatus.CREATED);
        if (paymentCreateSources.isEmpty()) return;
        List<String> paymentReadyOrderCodes = paymentCreateSources.stream().map(OrderPaymentCreateSource::orderCode)
                .toList();
        orderRepository.markStatusByOrderCodes(paymentReadyOrderCodes, OrderStatus.CREATED, OrderStatus.ORDER_SUCCESS);
        paymentManager.createPendingPayments(getList(paymentCreateSources));
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
