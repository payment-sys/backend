package com.v_payment.pay.product.service;

import com.v_payment.pay.order.service.OrderManager;
import com.v_payment.pay.payment.service.PaymentManager;
import com.v_payment.pay.product.domain.*;
import com.v_payment.pay.product.domain.entity.Product;
import com.v_payment.pay.product.domain.entity.ProductQuantityEvent;
import com.v_payment.pay.product.domain.entity.ProductQuantityEventPayload;
import com.v_payment.pay.product.domain.entity.ProductQuantityEventStatus;
import com.v_payment.pay.product.exception.ProductQuantityEventConsumeException;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQuantityEventService {
    private final Clock clock;
    private final RetryPolicy retryPolicy;
    private final OrderManager orderManager;
    private final PaymentManager paymentManager;
    private final ProductManager productManager;
    private final ProductRepository productRepository;
    private final ProductQuantityEventRepository productQuantityEventRepository;

    @Transactional
    public void consumeReadyEvent(int batchSize) {
        ProductQuantityEvents productQuantityEvents = ProductQuantityEvents.from(productQuantityEventRepository
                .findReadyProductQuantityEvents(ProductQuantityEventStatus.READY.toString(), batchSize));
        if (productQuantityEvents.isEmptyEvent()) return;

        consumeEvents(productQuantityEvents);
    }

    @Transactional
    public void consumeRetryEvent(int batchSize) {
        ProductQuantityEvents productQuantityEvents = ProductQuantityEvents.from(productQuantityEventRepository
                .findRetryableForUpdate(ProductQuantityEventStatus.RETRY.toString(), LocalDateTime.now(clock), batchSize));
        if (productQuantityEvents.isEmptyEvent()) return;

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

    private void consumeEvents(ProductQuantityEvents productQuantityEvents) {
        try {
            Map<Long, Product> products = productManager.findProductsMapForUpdate(
                    productQuantityEvents.getProductIdsDistinct());

            QuantityDecreasePlan quantityDecreasePlan = makePlan(products, productQuantityEvents);

            if(quantityDecreasePlan.hasDecreaseTotal()) {
                productRepository.decreaseProducts(quantityDecreasePlan.getDecreaseTotal());
            }

            markFailOrders(quantityDecreasePlan);

            List<PaymentManager.PendingPaymentCreateRequest> pendingPayment = makePendingPayments(
                    quantityDecreasePlan.getSuccess(), products);
            paymentManager.createPendingPayments(pendingPayment);

            productQuantityEventRepository.updateStatusByIds(productQuantityEvents.getIds(),
                    ProductQuantityEventStatus.CONSUMED, LocalDateTime.now(clock));
        } catch (Exception e) {
            throw new ProductQuantityEventConsumeException(
                    productQuantityEvents.getIds(), productQuantityEvents.getEvents(), e);
        }
    }

    private QuantityDecreasePlan makePlan(Map<Long, Product> products, ProductQuantityEvents productQuantityEvents) {
        ProductSnapShots productSnapShots = ProductSnapShots.from(products);
        QuantityDecreasePlanner quantityDecreasePlanner = QuantityDecreasePlanner.from(productSnapShots);

        return quantityDecreasePlanner.makeDecreasePlan(productQuantityEvents);
    }

    private void markFailOrders(QuantityDecreasePlan quantityDecreasePlan) {
        if (!quantityDecreasePlan.hasFailOrder()) return;
        boolean failUpdated = orderManager.markFailed(quantityDecreasePlan.getFailOrderCodes());

        if (!failUpdated) {
            log.error("product reservation failed, but order fail flag update failed. failOrderCodes={}",
                    quantityDecreasePlan.getFailOrderCodes());
            throw new IllegalStateException("order fail flag update failed");
        }
    }

    private List<PaymentManager.PendingPaymentCreateRequest> makePendingPayments(
            List<ProductQuantityEvent> successEvents, Map<Long, Product> products) {
        return successEvents.stream()
                .map(event -> {
                    ProductQuantityEventPayload payload = event.getPayload();

                    long amount = payload.getRequestedQuantities().entrySet().stream()
                            .mapToLong(entry -> products.get(entry.getKey()).getPrice() * entry.getValue())
                            .sum();

                    return new PaymentManager.PendingPaymentCreateRequest(
                            payload.getOrderCode(),
                            amount,
                            payload.getPaymentMethod()
                    );
                })
                .toList();
    }
}
