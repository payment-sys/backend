package com.v_payment.pay.product.service;

import com.v_payment.pay.product.entity.Product;
import com.v_payment.pay.product.entity.ProductBasicInfo;
import com.v_payment.pay.product.entity.ProductQuantityEvent;
import com.v_payment.pay.product.entity.ProductQuantityEventPayload;
import com.v_payment.pay.product.repository.ProductQuantityEventRepository;
import com.v_payment.pay.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductManager {
    private final Clock clock;
    private final ProductRepository productRepository;
    private final ProductQuantityEventRepository productQuantityEventRepository;

    public void createProductQuantityEvent(String orderCode, ProductQuantityEventPayload payload) {
        ProductQuantityEvent productQuantityEvent = ProductQuantityEvent.of(orderCode, payload, clock);

        productQuantityEventRepository.save(productQuantityEvent);
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

    public List<ProductBasicInfo> findProductBasicInfos(List<Long> productIds) {
        return productRepository.findProductBasicInfos(productIds);
    }

    public record ProductRestoreReq(
            Long productId,
            Integer quantity
    ) {
    }

}
