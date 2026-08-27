package com.v_payment.pay.order.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "orders")
public class Order {
    @Id
    @Column(name = "order_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(name = "order_code", nullable = false, unique = true)
    private String orderCode;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    private Long totalAmount;

    private LocalDateTime orderedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    public Order(String orderCode, OrderStatus orderStatus, Long totalAmount, LocalDateTime orderedAt) {
        this.orderCode = orderCode;
        this.orderStatus = orderStatus;
        this.totalAmount = totalAmount;
        this.orderedAt = orderedAt;
    }

    public void addItem(Long productId, String productName, Long unitPrice, Integer quantity) {
        OrderItem orderItem = OrderItem.create(this, productId, productName, unitPrice, quantity);
        orderItems.add(orderItem);
        totalAmount += orderItem.getOrderAmount();
    }

    public static Order create(String orderCode, LocalDateTime orderedAt) {
        return new Order(orderCode, OrderStatus.PENDING, 0L, orderedAt);
    }
}
