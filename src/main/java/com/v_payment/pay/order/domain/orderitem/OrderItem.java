package com.v_payment.pay.order.domain.orderitem;

import com.v_payment.pay.order.domain.order.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "order_item")
public class OrderItem {
    @Id
    @Column(name = "order_item_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Embedded
    private OrderItemInfo itemInfo;

    @Enumerated(EnumType.STRING)
    private OrderItemStatus status;

    private OrderItem(Order order, Long productId, String productName, Long unitPrice, Integer quantity, OrderItemStatus status) {
        this.order = order;
        this.itemInfo = OrderItemInfo.create(productId, productName, unitPrice, quantity);
        this.status = status;
    }

    public Long getProductId() {
        return itemInfo.getProductId();
    }

    public String getProductName() {
        return itemInfo.getProductName();
    }

    public Integer getQuantity() {
        return itemInfo.getQuantity();
    }

    public Long getUnitPrice() {
        return itemInfo.getUnitPrice();
    }

    public Long getOrderAmount() {
        return itemInfo.calculateOrderAmount();
    }

    public OrderItemInfo getOrderItemInfo() {    //방어적 복사
        return OrderItemInfo.create(itemInfo.getProductId(), itemInfo.getProductName(), itemInfo.getUnitPrice(),
                itemInfo.getQuantity());
    }

    public static OrderItem create(Order order, Long productId, String productName, Long unitPrice, Integer quantity) {
        return new OrderItem(order, productId, productName, unitPrice, quantity,  OrderItemStatus.PROCESSING);
    }
}
