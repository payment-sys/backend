package com.v_payment.pay.product.service;

import com.v_payment.pay.product.entity.ProductQuantityEvent;
import com.v_payment.pay.product.entity.ProductQuantityEventPayload;
import com.v_payment.pay.product.mq.ProductQuantityMessageQueue;
import com.v_payment.pay.product.repository.ProductQuantityEventRepository;
import com.v_payment.pay.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductManager {
    private final Clock clock;
    private final ProductQuantityMessageQueue mq;
    private final ProductRepository productRepository;
    private final ProductQuantityEventRepository productQuantityEventRepository;

    public void createProductQuantityEvent(String orderCode, ProductQuantityEventPayload payload) {
        ProductQuantityEvent productQuantityEvent = ProductQuantityEvent.of(orderCode, payload, clock);

        productQuantityEventRepository.save(productQuantityEvent);

        registerPutMqPayloadAfterCommit(payload);
    }

    private void registerPutMqPayloadAfterCommit(ProductQuantityEventPayload payload) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        mq.add(payload);
                    }
                }
        );
    }

    @Transactional
    public void restore(List<ProductRestoreReq> requests) {
        if (requests.isEmpty()) {
            return;
        }

        Map<Long, Integer> restoreQuantities = requests.stream()
                .collect(Collectors.toMap(
                        ProductRestoreReq::productId,
                        ProductRestoreReq::quantity,
                        Integer::sum
                ));

        restoreQuantities.forEach((productId, quantity) -> {
            int updatedRows = productRepository.changeStock(productId, quantity);
            if (updatedRows != 1) {
                throw new IllegalStateException("Product stock restore failed. productId = " + productId);
            }
        });
    }

    public record ProductRestoreReq(
            Long productId,
            Integer quantity
    ) {
    }
}
