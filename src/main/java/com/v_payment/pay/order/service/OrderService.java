package com.v_payment.pay.order.service;

import com.v_payment.pay.order.controller.dto.req.OrderCreateReq;
import com.v_payment.pay.order.controller.dto.req.OrderItemCreateReq;
import com.v_payment.pay.order.controller.dto.res.OrderCreateRes;
import com.v_payment.pay.order.entity.Order;
import com.v_payment.pay.order.repository.OrderRepository;
import com.v_payment.pay.product.entity.Product;
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

    /**
     * 1. 주문 생성(PENDING 상태)
     * 2. 상품 재고 차감 이벤트 생성
     * 3. orderItems 생성
     * 4-1. 상품 재고 차감 아웃박스 저장
     * 4-2. 상품 재고 차감 이벤트 mq에 add
     * 5. 주문 처리 성공 응답
     */
    @Transactional
    public OrderCreateRes create(OrderCreateReq req) {
        if(req.items().isEmpty()) throw new IllegalArgumentException("items cannot be create");

        //주문 생성
        String orderCode = UUID.randomUUID().toString();
        Order order = Order.create(orderCode, LocalDateTime.now(clock));
        orderRepository.save(order);

        //상품 재고 차감 이벤트 생성
        ProductQuantityEventPayload payload = ProductQuantityEventPayload.of(orderCode, req);

        //orderItems 생성
        List<Long> productIds = req.items().stream().map(OrderItemCreateReq::productId).toList();
        List<Product> products = productManager.findAllById(productIds);
        if(products.size() != productIds.size()) {throw new IllegalArgumentException();}
        for (Product p : products) {
            order.addItem(p.getId(), p.getName(), p.getPrice(), payload.getRequestedQuantities().get(p.getId()));
        }

        //상품 재고 차감 메시지 발행
        productManager.createProductQuantityEvent(orderCode, payload);

        //주문 처리 성공 응답
        return OrderCreateRes.from(orderCode);
    }
}
