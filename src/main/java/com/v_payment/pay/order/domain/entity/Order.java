package com.v_payment.pay.order.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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

    private boolean isFailed;

    private Long totalAmount;

    private LocalDateTime orderedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    private Order(String orderCode, boolean isFailed, Long totalAmount, LocalDateTime orderedAt) {
        this.orderCode = validateOrderCode(orderCode);
        this.isFailed = isFailed;
        this.totalAmount = validateTotalAmount(totalAmount);
        this.orderedAt = validateOrderedAt(orderedAt);
    }

    public void addItem(Long productId, String productName, Long unitPrice, Integer quantity) {
        OrderItem orderItem = OrderItem.create(this, productId, productName, unitPrice, quantity);
        orderItems.add(orderItem);
        totalAmount += orderItem.getOrderAmount();
    }

    public static Order create(String orderCode, LocalDateTime orderedAt) {
        return new Order(orderCode, false, 0L, orderedAt);
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

    private LocalDateTime validateOrderedAt(LocalDateTime orderedAt) {
        if (orderedAt == null) throw new IllegalArgumentException("orderedAt는 필수입니다!");
        return orderedAt;
    }
}
