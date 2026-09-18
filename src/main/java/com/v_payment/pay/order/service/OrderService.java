package com.v_payment.pay.order.service;

import com.v_payment.pay.global.exception.BusinessException;
import com.v_payment.pay.order.controller.dto.req.OrderCreateReq;
import com.v_payment.pay.order.controller.dto.res.OrderCreateRes;
import com.v_payment.pay.order.domain.OrderItemSources;
import com.v_payment.pay.order.domain.ReqQuantities;
import com.v_payment.pay.order.domain.entity.Order;
import com.v_payment.pay.order.exception.OrderException;
import com.v_payment.pay.order.repository.OrderRepository;
import com.v_payment.pay.payment.domain.entity.PaymentMethod;
import com.v_payment.pay.product.domain.ProductBasicInfo;
import com.v_payment.pay.product.domain.entity.ProductQuantityEventPayload;
import com.v_payment.pay.product.service.ProductManager;
import lombok.RequiredArgsConstructor;
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
    private final ProductManager productManager;

    @Transactional
    public OrderCreateRes create(OrderCreateReq req) {
        String orderCode = UUID.randomUUID().toString();

        ReqQuantities reqQuantities = ReqQuantities.from(req.items());

        createOrder(orderCode, reqQuantities);

        sendProductQuantityEventPayload(orderCode, req.paymentMethod(), reqQuantities);

        return OrderCreateRes.from(orderCode);
    }

    private void createOrder(String orderCode, ReqQuantities reqQuantities) {
        Order order = Order.create(orderCode, LocalDateTime.now(clock));
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

    private void sendProductQuantityEventPayload(String orderCode, PaymentMethod paymentMethod, ReqQuantities reqQuantities) {
        ProductQuantityEventPayload payload = ProductQuantityEventPayload.of(orderCode, paymentMethod, reqQuantities.getQuantityMap());
        productManager.createProductQuantityEvent(orderCode, payload);
    }
}
