package com.v_payment.pay.product.domain;

import com.v_payment.pay.payment.service.PaymentManager;
import com.v_payment.pay.product.domain.entity.Product;
import com.v_payment.pay.product.domain.entity.ProductQuantityEventPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductQuantityEventPlans {
    private final List<String> successOrderCodes = new ArrayList<>();
    private final List<String> failOrders = new ArrayList<>();
    private final Map<Long, Integer> decreaseTotal = new HashMap<>();
    private final List<PaymentManager.PendingPaymentCreateRequest> pendingPayments = new ArrayList<>();

    private ProductQuantityEventPlans() {
    }

    public static ProductQuantityEventPlans create() {
        return new ProductQuantityEventPlans();
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

    public List<String> successOrderCodes() {
        return successOrderCodes;
    }

    public List<String> failOrders() {
        return failOrders;
    }

    public Map<Long, Integer> decreaseTotal() {
        return decreaseTotal;
    }

    public List<PaymentManager.PendingPaymentCreateRequest> pendingPayments() {
        return pendingPayments;
    }
}
