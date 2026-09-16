package com.v_payment.pay.order.domain;

import com.v_payment.pay.order.controller.dto.req.OrderItemCreateReq;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReqQuantities {
    private final List<ReqQuantity> reqQuantities;
    private final Map<Long, ReqQuantity> valuesByProductId;

    private ReqQuantities(List<ReqQuantity> reqQuantities) {
        this.reqQuantities = validateValues(reqQuantities);
        this.valuesByProductId = toValuesByProductId(this.reqQuantities);
    }

    public List<Long> getProductIds() {
        return reqQuantities.stream()
                .map(ReqQuantity::getProductId)
                .toList();
    }

    public boolean hasAllProducts(int size) {
        return reqQuantities.size() == size;
    }

    public int getQuantityByProductId(Long productId) {
        ReqQuantity reqQuantity = valuesByProductId.get(productId);
        if (reqQuantity == null) throw new IllegalArgumentException("요청하지 않은 상품입니다.");
        return reqQuantity.getQuantity();
    }

    public Map<Long, Integer> getQuantityMap() {
        return Collections.unmodifiableMap(reqQuantities.stream()
                .collect(Collectors.toMap(ReqQuantity::getProductId, ReqQuantity::getQuantity)));
    }

    public static ReqQuantities from(List<OrderItemCreateReq> reqs) {
        if (reqs == null) throw new IllegalArgumentException("주문 상품 목록은 필수입니다.");
        return new ReqQuantities(reqs.stream()
                .map(ReqQuantity::from)
                .toList());
    }

    private List<ReqQuantity> validateValues(List<ReqQuantity> values) {
        if (values == null) throw new IllegalArgumentException("주문 상품 목록은 필수입니다.");
        if (values.isEmpty()) throw new IllegalArgumentException("주문 상품은 하나 이상이어야 합니다.");
        return List.copyOf(values);
    }

    private Map<Long, ReqQuantity> toValuesByProductId(List<ReqQuantity> values) {
        try {
            return values.stream()
                    .collect(Collectors.toUnmodifiableMap(ReqQuantity::getProductId, Function.identity()));
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("같은 상품을 중복해서 주문할 수 없습니다.", e);
        }
    }
}
