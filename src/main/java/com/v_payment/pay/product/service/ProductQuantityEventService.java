package com.v_payment.pay.product.service;

import com.v_payment.pay.order.service.OrderManager;
import com.v_payment.pay.payment.service.PaymentManager;
import com.v_payment.pay.product.entity.Product;
import com.v_payment.pay.product.entity.ProductQuantityEvent;
import com.v_payment.pay.product.entity.ProductQuantityEventPayload;
import com.v_payment.pay.product.entity.ProductQuantityEventStatus;
import com.v_payment.pay.product.exception.ProductQuantityEventConsumeException;
import com.v_payment.pay.product.mq.RetryPolicy;
import com.v_payment.pay.product.repository.ProductQuantityEventRepository;
import com.v_payment.pay.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQuantityEventService {
    private final Clock clock;
    private final RetryPolicy retryPolicy;
    private final OrderManager orderManager;
    private final PaymentManager paymentManager;
    private final ProductRepository productRepository;
    private final ProductQuantityEventRepository productQuantityEventRepository;

    @Transactional
    public void consumeReadyEvent(int batchSize) {
        List<ProductQuantityEvent> productQuantityEvents = productQuantityEventRepository.findByProductQuantityEventStatusOrderByIdAsc(
                        ProductQuantityEventStatus.READY.toString(), batchSize);
        if (productQuantityEvents.isEmpty()) return;

        consumeEvents(productQuantityEvents);
    }

    @Transactional
    public void consumeRetryEvent(int batchSize) {
        List<ProductQuantityEvent> productQuantityEvents = productQuantityEventRepository.findRetryableForUpdate(
                        ProductQuantityEventStatus.RETRY.toString(), LocalDateTime.now(clock), batchSize);
        if (productQuantityEvents.isEmpty()) return;
        consumeEvents(productQuantityEvents);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRetry(List<ProductQuantityEvent> events, List<Long> eventIds) {
        int retryCount = events.stream()
                .map(ProductQuantityEvent::getRetryCount)
                .max(Integer::compareTo)
                .orElse(0);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime nextAttemptTime = retryPolicy.nextAttemptTime(now, retryCount);

        productQuantityEventRepository.markRetryByIds(eventIds, nextAttemptTime, now);
    }

    private void consumeEvents(List<ProductQuantityEvent> productQuantityEvents) {
        List<Long> eventIds = productQuantityEvents.stream()
                .map(ProductQuantityEvent::getId)
                .toList();

        try {
            productQuantityEventRepository.updateStatusByIds(
                    eventIds,
                    ProductQuantityEventStatus.CONSUMED,
                    LocalDateTime.now(clock)
            );

            List<ProductQuantityEventPayload> payloads = productQuantityEvents.stream()
                    .map(ProductQuantityEvent::getPayload)
                    .toList();
            List<Long> productIds = payloads.stream()
                    .flatMap(payload -> payload.getRequestedQuantities().keySet().stream())
                    .distinct()
                    .toList();
            Map<Long, Product> products = productRepository.findAllByIdInForUpdate(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, product -> product));

            ProductQuantityEventPlans plans = makePlan(products, payloads);
            productRepository.decreaseProducts(plans.decreaseTotal());
            markFailOrders(plans);
            paymentManager.createPendingPayments(plans.pendingPayments());
        } catch (Exception e) {
            throw new ProductQuantityEventConsumeException(eventIds, productQuantityEvents, e);
        }
    }

    private ProductQuantityEventPlans makePlan(Map<Long, Product> products, List<ProductQuantityEventPayload> payloads) {
        ProductQuantityEventPlans plans = ProductQuantityEventPlans.create();
        for (ProductQuantityEventPayload payload : payloads) {
            if (canReserve(payload, products, plans)) {
                plans.success(payload, products);
                continue;
            }
            plans.fail(payload.getOrderCode());
        }
        return plans;
    }

    private boolean canReserve(ProductQuantityEventPayload payload, Map<Long, Product> products, ProductQuantityEventPlans plans) {
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

    private void markFailOrders(ProductQuantityEventPlans plans) {
        if (plans.failOrders().isEmpty()) {
            return;
        }
        boolean failUpdated = orderManager.markFailed(plans.failOrders());

        if (!failUpdated) {
            log.error("product reservation failed, but order fail flag update failed. failOrderCodes={}",
                    plans.failOrders());
            throw new IllegalStateException("order fail flag update failed");
        }
    }
}
