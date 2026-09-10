package com.v_payment.pay.product.mq;

import com.v_payment.pay.payment.service.PaymentManager;
import com.v_payment.pay.product.entity.Product;
import com.v_payment.pay.product.entity.ProductQuantityEventPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ProductQuantityEventPlans(
        List<String> successOrderCodes,
        List<String> failOrders,
        Map<Long, Integer> decreaseTotal,
        List<PaymentManager.PendingPaymentCreateRequest> pendingPayments
) {
    public static ProductQuantityEventPlans create() {
        return new ProductQuantityEventPlans(new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new ArrayList<>());
    }

    public void success(ProductQuantityEventPayload payload, Map<Long, Product> products) {
        successOrderCodes.add(payload.getOrderCode());

        long amount = 0L;
        for (Map.Entry<Long, Integer> entry : payload.getRequestedQuantities().entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();
            decreaseTotal.merge(productId, -quantity, Integer::sum);

            Product product = products.get(productId);
            amount += product.getPrice() * quantity;
        }

        pendingPayments.add(new PaymentManager.PendingPaymentCreateRequest(
                payload.getOrderCode(),
                amount,
                payload.getPaymentMethod()
        ));
    }

    public void fail(String orderCode) {
        failOrders.add(orderCode);
    }
}
