package com.v_payment.pay.product.service;

import com.v_payment.pay.order.service.OrderManager;
import com.v_payment.pay.payment.service.PaymentManager;
import com.v_payment.pay.product.domain.ProductQuantityEvents;
import com.v_payment.pay.product.domain.ProductSnapShots;
import com.v_payment.pay.product.domain.QuantityDecreasePlan;
import com.v_payment.pay.product.domain.QuantityDecreasePlanner;
import com.v_payment.pay.product.domain.entity.Product;
import com.v_payment.pay.product.domain.entity.ProductQuantityEvent;
import com.v_payment.pay.product.domain.entity.ProductQuantityEventPayload;
import com.v_payment.pay.product.domain.entity.ProductQuantityEventStatus;
import com.v_payment.pay.product.repository.ProductQuantityEventRepository;
import com.v_payment.pay.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductQuantityEventService {
    private final Clock clock;
    private final OrderManager orderManager;
    private final PaymentManager paymentManager;
    private final ProductManager productManager;
    private final ProductRepository productRepository;
    private final ProductQuantityEventRepository productQuantityEventRepository;

    @Transactional
    public void consumeReadyEvent(int batchSize) {
        List<ProductQuantityEvent> readyProductQuantityEvents = productQuantityEventRepository
                .findReadyProductQuantityEvents(ProductQuantityEventStatus.READY.toString(), batchSize);

        ProductQuantityEvents productQuantityEvents = ProductQuantityEvents.from(readyProductQuantityEvents);
        if (productQuantityEvents.isEmptyEvent()) return;

        Map<Long, Product> products = productManager.findProductsMapForUpdate(
                productQuantityEvents.getProductIdsDistinct());

        QuantityDecreasePlan quantityDecreasePlan = makePlan(products, productQuantityEvents);

        if (quantityDecreasePlan.hasDecreaseTotal()) {
            productRepository.decreaseProducts(quantityDecreasePlan.getDecreaseTotal());
        }

        markLackQuantities(quantityDecreasePlan);

        createPendingPayments(quantityDecreasePlan, products);

        productQuantityEventRepository.updateStatusByIds(productQuantityEvents.getIds(),
                ProductQuantityEventStatus.CONSUMED, LocalDateTime.now(clock));
    }

    private QuantityDecreasePlan makePlan(Map<Long, Product> products, ProductQuantityEvents productQuantityEvents) {
        ProductSnapShots productSnapShots = ProductSnapShots.from(products);
        QuantityDecreasePlanner quantityDecreasePlanner = QuantityDecreasePlanner.from(productSnapShots);

        return quantityDecreasePlanner.makeDecreasePlan(productQuantityEvents);
    }

    private void markLackQuantities(QuantityDecreasePlan quantityDecreasePlan) {
        if (!quantityDecreasePlan.hasFailOrder()) return;
        boolean failUpdated = orderManager.markLackQuantities(quantityDecreasePlan.getFailOrderCodes());

        if (!failUpdated) {
            throw new IllegalStateException(
                    "주문 재고부족 상태 업데이트를 실패했습니다. failOrderCodes="
                            + quantityDecreasePlan.getFailOrderCodes());
        }
    }

    private void createPendingPayments(QuantityDecreasePlan quantityDecreasePlan, Map<Long, Product> products) {
        paymentManager.createPendingPayments(quantityDecreasePlan.getSuccess().stream()
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
                .toList());
    }
}
