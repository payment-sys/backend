package com.v_payment.pay.order.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    private boolean isFailed;

    private Long totalAmount;

    private LocalDateTime orderedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    public Order(String orderCode, boolean isFailed, Long totalAmount, LocalDateTime orderedAt) {
        this.orderCode = orderCode;
        this.isFailed = isFailed;
        this.totalAmount = totalAmount;
        this.orderedAt = orderedAt;
    }

    public void addItem(Long productId, String productName, Long unitPrice, Integer quantity) {
        OrderItem orderItem = OrderItem.create(this, productId, productName, unitPrice, quantity);
        orderItems.add(orderItem);
        totalAmount += orderItem.getOrderAmount();
    }

    public static Order create(String orderCode, LocalDateTime orderedAt) {
        return new Order(orderCode, false, 0L, orderedAt);
    }
}
