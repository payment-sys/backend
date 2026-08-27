package com.v_payment.pay.product.service;

import com.v_payment.pay.product.entity.Product;
import com.v_payment.pay.product.entity.ProductQuantityEvent;
import com.v_payment.pay.product.entity.ProductQuantityEventPayload;
import com.v_payment.pay.product.entity.ProductQuantityEventStatus;
import com.v_payment.pay.product.mq.ProductQuantityMessageQueue;
import com.v_payment.pay.product.repository.ProductQuantityEventRepository;
import com.v_payment.pay.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductManager {
    private final Clock clock;
    private final ProductQuantityMessageQueue mq;
    private final ProductRepository productRepository;
    private final TransactionTemplate transactionTemplate;
    private final ProductQuantityEventRepository productQuantityEventRepository;

    /**
     * 1. 아웃박스 생성
     * 2. 아웃박스 저장
     * 3. 메시지 발행 -> 트랜잭션 commit 이후 메시지 발행
     */
    public void createProductQuantityEvent(String orderCode, ProductQuantityEventPayload payload) {
        ProductQuantityEvent productQuantityEvent = ProductQuantityEvent.of(orderCode, payload, clock);

        productQuantityEventRepository.save(productQuantityEvent);

        registerPutMqPayloadAfterCommit(payload, productQuantityEvent.getId());
    }

    private void registerPutMqPayloadAfterCommit(ProductQuantityEventPayload payload, Long productQuantityEventId) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try{
                            boolean isAdded = mq.add(payload);
                            if(!isAdded) throw new IllegalStateException("메시지 큐에 이벤트를 넣지 못했습니다.");
                        } catch (Exception e) {
                            log.warn("메시지 발행을 실패했습니다. productQuantityEventId = {}", productQuantityEventId, e);
                            transactionTemplate.executeWithoutResult(status -> productQuantityEventRepository.findById(productQuantityEventId).ifPresent(productQuantityEvent -> productQuantityEvent.updateStatus(ProductQuantityEventStatus.FAILED)));
                        }
                        transactionTemplate.executeWithoutResult(status ->
                                productQuantityEventRepository.findById(productQuantityEventId)
                                        .ifPresent(productQuantityEvent ->
                                                productQuantityEvent.updateStatus(ProductQuantityEventStatus.PUBLISHED)));
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

    public List<Product> findAllById(List<Long> productIds) {
        return productRepository.findAllById(productIds);
    }

    public record ProductRestoreReq(
            Long productId,
            Integer quantity
    ) {
    }
}
