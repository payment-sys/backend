package com.v_payment.pay.order.service;

import com.v_payment.pay.order.controller.dto.req.OrderCreateReq;
import com.v_payment.pay.order.controller.dto.req.OrderItemCreateReq;
import com.v_payment.pay.order.controller.dto.res.OrderCreateRes;
import com.v_payment.pay.order.entity.Order;
import com.v_payment.pay.order.repository.OrderRepository;
import com.v_payment.pay.product.entity.ProductBasicInfo;
import com.v_payment.pay.product.entity.ProductQuantityEventPayload;
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
        if (req.items().isEmpty()) throw new IllegalArgumentException("items cannot be create");

        String orderCode = UUID.randomUUID().toString();
        Order order = Order.create(orderCode, LocalDateTime.now(clock));
        orderRepository.save(order);

        ProductQuantityEventPayload payload = ProductQuantityEventPayload.of(orderCode, req);

        List<Long> productIds = req.items().stream().map(OrderItemCreateReq::productId).toList();
        List<ProductBasicInfo> productBasicInfos = productManager.findProductBasicInfos(productIds);
        if (productBasicInfos.size() != productIds.size()) throw new IllegalArgumentException();
        for (ProductBasicInfo p : productBasicInfos) {
            order.addItem(p.productId(), p.name(), p.price(), payload.getRequestedQuantities().get(p.productId()));
        }

        productManager.createProductQuantityEvent(
                orderCode,
                ProductQuantityEventPayload.of(orderCode, req, order.getTotalAmount())
        );

        return OrderCreateRes.from(orderCode);
    }
}
