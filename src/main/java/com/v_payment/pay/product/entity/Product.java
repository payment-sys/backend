package com.v_payment.pay.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "product")
public class Product {
    @Id
    @Column(name = "product_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Long price;

    private Integer stockQuantity;

    private Product(String name, Long price, Integer stockQuantity) {
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    public void subtractQuantity(Integer quantity) {
        if (stockQuantity < quantity) {
            throw new IllegalArgumentException("Insufficient stock.");
        }
        stockQuantity -= quantity;
    }

    public static Product create(String name, Long price, Integer stockQuantity) {
        return new Product(name, price, stockQuantity);
    }
}
