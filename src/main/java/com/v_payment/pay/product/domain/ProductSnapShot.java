package com.v_payment.pay.product.domain;

public class ProductSnapShot {
    private int remainQuantity;

    ProductSnapShot(int remainQuantity) {
        this.remainQuantity = remainQuantity;
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
