package com.v_payment.pay.order.domain;

import com.v_payment.pay.product.domain.ProductBasicInfo;

import java.util.List;
import java.util.function.Consumer;

public class OrderItemSources {
    private List<OrderItemSource> orderItemSources;

    private OrderItemSources(List<OrderItemSource> orderItemSources) {
        this.orderItemSources = orderItemSources;
    }

    public int getOrderItemCount() {
        return orderItemSources.size();
    }

    public void forEach(Consumer<OrderItemSource> consumer) {
        orderItemSources.forEach(consumer);
    }

    public static OrderItemSources of(List<ProductBasicInfo> productBasicInfos, ReqQuantities reqQuantities) {
        return new OrderItemSources(productBasicInfos.stream()
                .map(p -> OrderItemSource.create(p.productId(), p.name(), p.price(), reqQuantities.getQuantityByProductId(p.productId())))
                .toList()
        );
    }
}
