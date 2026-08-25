package com.v_payment.pay.product.mq;

import com.v_payment.pay.order.service.OrderManager;
import com.v_payment.pay.payment.service.PaymentManager;
import com.v_payment.pay.product.entity.Product;
import com.v_payment.pay.product.entity.ProductQuantityEventPayload;
import com.v_payment.pay.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 주문(code) 안에 상품들(map,Long) 안에 상품(Long, Integer)
@Component
@RequiredArgsConstructor
public class MqConsumeHandler {
    private final ProductRepository productRepository;
    private final OrderManager orderManager;
    private final PaymentManager paymentManager;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void reserve(List<ProductQuantityEventPayload> payloads) {
        List<Long> productIds = payloads.stream()
                .map(ProductQuantityEventPayload::getRequestedQuantities)
                .flatMap(rqs -> rqs.keySet().stream())
                .distinct()
                .toList();

        Map<Long, Product> products = productRepository.findAllByIdInForUpdate(productIds).stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        product -> product
                ));

        Plans plans = new Plans(new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new ArrayList<>());
        payloads.forEach(p -> {
                    Map<Long, Integer> requestedQuantities = p.getRequestedQuantities();

                    boolean can = true;
                    for(Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
                        Long productId = entry.getKey();
                        int quantity = entry.getValue();

                        Product product = products.get(productId);
                        if(product == null) {
                            can = false;
                            break;
                        }

                        int remainingStock = product.getStockQuantity()
                                + plans.decreaseTotal().getOrDefault(productId, 0);
                        if(remainingStock < quantity) {
                            can = false;
                            break;
                        }
                    }

                    if(can) {
                        plans.success(p, products);
                        return;
                    }
                    plans.fail(p.getOrderCode());
                });

        decreaseProducts(plans);
        orderManager.updateProductQuantityReservationStatus(plans.successOrderCodes(), plans.failOrders());
        paymentManager.createPendingPayments(plans.pendingPayments());
    }

    private void decreaseProducts(Plans plans) {
        if (plans.decreaseTotal().isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                """
                update product
                set stock_quantity = stock_quantity + ?
                where product_id = ?
                """,
                plans.decreaseTotal().entrySet(),
                plans.decreaseTotal().size(),
                (ps, entry) -> {
                    ps.setInt(1, entry.getValue());
                    ps.setLong(2, entry.getKey());
                }
        );
    }

    record Plans(
            List<String> successOrderCodes,
            List<String> failOrders,
            Map<Long, Integer> decreaseTotal,
            List<PaymentManager.PendingPaymentCreateRequest> pendingPayments
    ) {
        public void success(ProductQuantityEventPayload payload, Map<Long, Product> products) {
            successOrderCodes.add(payload.getOrderCode());

            long amount = 0L;
            for(Map.Entry<Long, Integer> entry : payload.getRequestedQuantities().entrySet()) {
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
}
