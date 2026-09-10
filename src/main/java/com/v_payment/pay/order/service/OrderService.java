package com.v_payment.pay.order.service;

import com.v_payment.pay.global.exception.BusinessException;
import com.v_payment.pay.order.controller.dto.req.OrderCreateReq;
import com.v_payment.pay.order.controller.dto.req.OrderItemCreateReq;
import com.v_payment.pay.order.controller.dto.res.OrderCreateRes;
import com.v_payment.pay.order.entity.Order;
import com.v_payment.pay.order.exception.OrderException;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final Clock clock;
    private final OrderRepository orderRepository;
    private final ProductManager productManager;

    @Transactional
    public OrderCreateRes create(OrderCreateReq req) {
        //기본 값 준비
        String orderCode = UUID.randomUUID().toString();
        Map<Long, Integer> requestedQuantities = req.items().stream()
                .collect(Collectors.toMap(OrderItemCreateReq::productId, OrderItemCreateReq::quantity));

        //주문 생성
        Order order = Order.create(orderCode, LocalDateTime.now(clock));
        List<Long> productIds = req.items().stream().map(OrderItemCreateReq::productId).toList();
        List<ProductBasicInfo> productBasicInfos = productManager.findProductBasicInfos(productIds);
        if (productBasicInfos.size() != productIds.size()) throw new BusinessException(OrderException.ORDER_ITEM_NOT_FOUND);
        for (ProductBasicInfo p : productBasicInfos) {
            order.addItem(p.productId(), p.name(), p.price(), requestedQuantities.get(p.productId()));
        }
        orderRepository.save(order);

        //재고 차감 이벤트 버퍼에 저장
        ProductQuantityEventPayload payload = ProductQuantityEventPayload.of(orderCode, req.paymentMethod(),
                requestedQuantities);
        productManager.createProductQuantityEvent(orderCode, payload);

        return OrderCreateRes.from(orderCode);
    }
}
