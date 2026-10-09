package com.v_payment.pay.order.domain.order;

import com.v_payment.pay.order.entrypoint.dto.req.OrderItemCreateReq;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ReqQuantity {
    private Long productId;
    private Integer quantity;

    private ReqQuantity(Long productId, Integer quantity) {
        this.productId = validateProductId(productId);
        this.quantity = validateQuantity(quantity);
    }

    public static ReqQuantity of(Long productId, Integer quantity) {
        return new ReqQuantity(productId, quantity);
    }

    public static ReqQuantity from(OrderItemCreateReq req) {
        if (req == null) throw new IllegalArgumentException("주문 수량 요청은 필수입니다.");
        return new ReqQuantity(req.productId(), req.quantity());
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    private Long validateProductId(Long productId) {
        if (productId == null) throw new IllegalArgumentException("상품 ID는 필수입니다.");
        return productId;
    }

    private Integer validateQuantity(Integer quantity) {
        if (quantity == null) throw new IllegalArgumentException("주문 수량은 필수입니다.");
        if (quantity <= 0) throw new IllegalArgumentException("주문 수량은 0보다 커야 합니다.");
        return quantity;
    }
}
