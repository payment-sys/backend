package com.v_payment.pay.product.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(
        name = "product",
        indexes = {
                @Index(name = "idx_product_order_item_snapshot", columnList = "product_id, name, price")
        }
)
public class Product {
    @Id
    @Column(name = "product_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Long price;

    private Integer stockQuantity;

    private Product(String name, Long price, Integer stockQuantity) {
        this.name = validateName(name);
        this.price = validatePrice(price);
        this.stockQuantity = validateStockQuantity(stockQuantity);
    }

    public static Product create(String name, Long price, Integer stockQuantity) {
        return new Product(name, price, stockQuantity);
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name은 필수입니다.");
        return name;
    }

    private Long validatePrice(Long price) {
        if (price == null) throw new IllegalArgumentException("price는 null일 수 없습니다.");
        if (price < 0) throw new IllegalArgumentException("price는 음수일 수 없습니다.");
        return price;
    }

    private Integer validateStockQuantity(Integer stockQuantity) {
        if (stockQuantity == null) throw new IllegalArgumentException("stockQuantity는 필수 입니다.");
        if (stockQuantity < 0) throw new IllegalArgumentException("stockQuantity는 음수일 수 없습니다.");
        return stockQuantity;
    }
}
