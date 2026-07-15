package com.v_payment.pay.order.service;

import com.v_payment.pay.order.controller.dto.req.OrderCreateReq;
import com.v_payment.pay.order.controller.dto.req.OrderItemCreateReq;
import com.v_payment.pay.order.controller.dto.res.OrderCreateRes;
import com.v_payment.pay.order.entity.Order;
import com.v_payment.pay.order.repository.OrderRepository;
import com.v_payment.pay.payment.service.PaymentManager;
import com.v_payment.pay.product.service.ProductManager;
import com.v_payment.pay.product.service.ReservedProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final Clock clock;
    private final OrderRepository orderRepository;
    private final ProductManager productManager;
    private final PaymentManager paymentManager;

    @Transactional
    public OrderCreateRes create(OrderCreateReq req) {
        List<ReservedProduct> reservedProducts = reserveProducts(req.items());

        Order savedOrder = createOrderWithReservedProducts(reservedProducts);

        paymentManager.createPendingPayment(savedOrder.getOrderCode(), savedOrder.getTotalAmount(), req.paymentMethod());

        return OrderCreateRes.from(savedOrder);
    }

    private List<ReservedProduct> reserveProducts(List<OrderItemCreateReq> items) {
        List<ProductManager.ProductReservationReq> productReservationReq = items.stream()
                .map(item -> new ProductManager.ProductReservationReq(item.productId(), item.quantity()))
                .toList();
        return productManager.reserve(productReservationReq);
    }

    private Order createOrderWithReservedProducts(List<ReservedProduct> reservedProducts) {
        Order order = Order.create(LocalDateTime.now(clock));
        reservedProducts.forEach(product ->
                order.addItem(product.productId(), product.productName(), product.unitPrice(), product.quantity())
        );
        return orderRepository.save(order);
    }
}
