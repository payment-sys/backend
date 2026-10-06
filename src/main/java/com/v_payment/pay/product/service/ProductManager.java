package com.v_payment.pay.product.service;

import com.v_payment.pay.product.domain.ProductBasicInfo;
import com.v_payment.pay.product.domain.entity.Product;
import com.v_payment.pay.product.repository.ProductRepository;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductManager {
    private final ProductRepository productRepository;

    @Transactional("productTransactionManager")
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
                throw new IllegalStateException("상품 재고 복구에 실패했습니다. productId = " + productId);
            }
        });
    }

    @WithSpan("product.ProductManager.findProductBasicInfos")
    @Transactional(value = "productTransactionManager", readOnly = true)
    public List<ProductBasicInfo> findProductBasicInfos(List<Long> productIds) {
        return productRepository.findProductBasicInfos(productIds);
    }

    @Transactional("productTransactionManager")
    public Map<Long, Product> findProductsMapForUpdate(Collection<Long> productIds) {
        return productRepository.findAllByIdInForUpdate(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
    }

    public record ProductRestoreReq(
            Long productId,
            Integer quantity
    ) {
    }

}
