package com.v_payment.pay.product.service;

import com.v_payment.pay.global.exception.BusinessException;
import com.v_payment.pay.product.entity.Product;
import com.v_payment.pay.product.exception.ProductException;
import com.v_payment.pay.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductManager {
    private final ProductRepository productRepository;

    public List<ReservedProduct> reserve(List<ProductReservationReq> requests) {
        List<Long> productIds = requests.stream().map(ProductReservationReq::productId).sorted().toList();
        List<Product> products = productRepository.findAllByIdInForUpdate(productIds);
        if (isNotFoundProductsExist(productIds, products)) throw new BusinessException(ProductException.PRODUCT_NOT_FOUND);

        Map<Long, Product> productById = products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));

        return requests.stream()
                .map(request -> reserve(productById.get(request.productId()), request.quantity()))
                .toList();
    }

    private boolean isNotFoundProductsExist(List<Long> productIds, List<Product> products) {
        return productIds.size() != products.size();
    }

    private ReservedProduct reserve(Product product, Integer quantity) {
        try {
            product.subtractQuantity(quantity);
            return ReservedProduct.of(product, quantity);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ProductException.OUT_OF_STOCK);
        }
    }

    public record ProductReservationReq(
            Long productId,
            Integer quantity
    ) {
    }
}
