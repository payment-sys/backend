package com.v_payment.pay.order.domain.order;

import com.v_payment.pay.order.domain.orderitem.OrderItemSources;
import com.v_payment.pay.order.domain.orderitem.OrderItem;
import com.v_payment.pay.payment.domain.entity.PaymentMethod;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "orders")
public class Order {
    @Id
    @Column(name = "order_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(name = "order_code", nullable = false, unique = true)
    private String orderCode;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Long totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    private LocalDateTime orderedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    private Order(String orderCode, OrderStatus status, Long totalAmount, PaymentMethod paymentMethod, LocalDateTime orderedAt) {
        this.orderCode = validateOrderCode(orderCode);
        this.status = validateStatus(status);
        this.totalAmount = validateTotalAmount(totalAmount);
        this.paymentMethod = validatePaymentMethod(paymentMethod);
        this.orderedAt = validateOrderedAt(orderedAt);
    }

    public void addItems(OrderItemSources orderItemSources) {
        orderItemSources.forEach(ois -> {
            OrderItem orderItem = OrderItem.create(this, ois.getProductId(), ois.getName(), ois.getPrice(),
                    ois.getQuantity());
            orderItems.add(orderItem);
            totalAmount += orderItem.getOrderAmount();
        });
    }

    public static Order create(String orderCode, PaymentMethod paymentMethod, LocalDateTime orderedAt) {
        return new Order(orderCode, OrderStatus.CREATED, 0L, paymentMethod, orderedAt);
    }

    public static Order create(String orderCode, LocalDateTime orderedAt) {
        return create(orderCode, PaymentMethod.CARD, orderedAt);
    }

    private String validateOrderCode(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) throw new IllegalArgumentException("orderCode는 필수입니다!");
        return orderCode;
    }

    private Long validateTotalAmount(Long totalAmount) {
        if (totalAmount == null) throw new IllegalArgumentException("totalAmount는 필수입니다!");
        if (totalAmount < 0) throw new IllegalArgumentException("총 가격은 음수일 수 없습니다.");
        return totalAmount;
    }

    private OrderStatus validateStatus(OrderStatus status) {
        if (status == null) throw new IllegalArgumentException("주문 상태는 필수입니다!");
        return status;
    }

    private PaymentMethod validatePaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) throw new IllegalArgumentException("paymentMethod is required.");
        return paymentMethod;
    }

    private LocalDateTime validateOrderedAt(LocalDateTime orderedAt) {
        if (orderedAt == null) throw new IllegalArgumentException("orderedAt는 필수입니다!");
        return orderedAt;
    }
}
