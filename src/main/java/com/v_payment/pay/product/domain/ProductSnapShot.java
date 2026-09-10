package com.v_payment.pay.product.domain;

public class ProductSnapShot {
    private int remainQuantity;
    private Long productId;
    private String name;
    private Long price;

    ProductSnapShot(int remainQuantity, Long productId, String name, Long price) {
        this.remainQuantity = remainQuantity;
        this.productId = productId;
        this.name = name;
        this.price = price;
    }

    public boolean canDecrease(int quantity) {
        return remainQuantity - quantity >= 0;
    }

    void decrease(int quantity) {
        if (!canDecrease(quantity)) {
            throw new IllegalArgumentException("재고가 부족합니다!");
        }
        this.remainQuantity -= quantity;
    }
}
