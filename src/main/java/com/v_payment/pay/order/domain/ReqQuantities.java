package com.v_payment.pay.order.domain;

import com.v_payment.pay.order.controller.dto.req.OrderItemCreateReq;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReqQuantities {
    private final String orderCode;
    private final List<ReqQuantity> reqQuantities;
    private final Map<Long, ReqQuantity> valuesByProductId;

    private ReqQuantities(String orderCode, List<ReqQuantity> reqQuantities) {
        this.orderCode = orderCode;
        this.reqQuantities = validateValues(reqQuantities);
        this.valuesByProductId = toValuesByProductId(this.reqQuantities);
    }

    public String getOrderCode() {
        return orderCode;
    }

    public List<ReqQuantity> getReqQuantities() {
        return reqQuantities;
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

    public static ReqQuantities of(List<OrderItemCreateReq> reqs) {
        return from(reqs);
    }

    public static ReqQuantities of(String orderCode, List<OrderItemCreateReq> reqs) {
        if (orderCode == null || orderCode.isBlank()) throw new IllegalArgumentException("orderCode is required.");
        if (reqs == null) throw new IllegalArgumentException("order items are required.");
        return new ReqQuantities(orderCode, reqs.stream()
                .map(ReqQuantity::from)
                .toList());
    }

    public static ReqQuantities from(List<OrderItemCreateReq> reqs) {
        if (reqs == null) throw new IllegalArgumentException("주문 상품 목록은 필수입니다.");
        return new ReqQuantities(null, reqs.stream()
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
