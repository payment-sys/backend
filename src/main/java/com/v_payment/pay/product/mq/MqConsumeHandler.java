package com.v_payment.pay.product.mq;

import com.v_payment.pay.order.service.OrderManager;
import com.v_payment.pay.payment.service.PaymentManager;
import com.v_payment.pay.product.entity.Product;
import com.v_payment.pay.product.entity.ProductQuantityEventPayload;
import com.v_payment.pay.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqConsumeHandler {
    private final ProductRepository productRepository;
    private final OrderManager orderManager;
    private final PaymentManager paymentManager;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void handle(List<ProductQuantityEventPayload> payloads) {
        Map<Long, Product> products = findProductByPayloads(payloads);

        ConsumePlans plans = makePlan(products, payloads);

        decreaseProducts(plans);

        markFailOrders(plans);

        paymentManager.createPendingPayments(plans.pendingPayments());
    }

    private void markFailOrders(ConsumePlans plans) {
        if(plans.failOrders().isEmpty()) return;
        boolean failUpdated = orderManager.markFailed(plans.failOrders());

        if (!failUpdated) {
            log.error("product reservation failed, but order fail flag update failed. failOrderCodes={}",
                    plans.failOrders());
            throw new IllegalStateException("order fail flag update failed");
        }
    }

    private Map<Long, Product> findProductByPayloads(List<ProductQuantityEventPayload> payloads) {
        List<Long> productIds = payloads.stream()
                .map(ProductQuantityEventPayload::getRequestedQuantities)
                .flatMap(rqs -> rqs.keySet().stream())
                .distinct()
                .toList();

        return productRepository.findAllByIdInForUpdate(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
    }

    private ConsumePlans makePlan(Map<Long, Product> products, List<ProductQuantityEventPayload> payloads) {
        ConsumePlans plans = ConsumePlans.create();
        for (ProductQuantityEventPayload payload : payloads) {
            if (canReserve(payload, products, plans)) {
                plans.success(payload, products);
                continue;
            }
            plans.fail(payload.getOrderCode());
        }
        return plans;
    }

    private void decreaseProducts(ConsumePlans plans) {
        if (plans.decreaseTotal().isEmpty()) return;

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

    private boolean canReserve(ProductQuantityEventPayload payload, Map<Long, Product> products, ConsumePlans plans) {
        for (Map.Entry<Long, Integer> requestEntry : payload.getRequestedQuantities().entrySet()) {
            Long productId = requestEntry.getKey();
            Integer quantity = requestEntry.getValue();

            Product productForDecrease = products.get(productId);
            if (productForDecrease == null) {
                return false;
            }

            int remainingStock = productForDecrease.getStockQuantity()
                    + plans.decreaseTotal().getOrDefault(productId, 0);
            if (remainingStock < quantity) {
                return false;
            }
        }

        return true;
    }
}
