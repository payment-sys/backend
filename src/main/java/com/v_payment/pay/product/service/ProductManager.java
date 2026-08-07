package com.v_payment.pay.product.service;

import com.v_payment.pay.product.cache.ProductCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductManager { //
    private final ProductCache productCache;

    public List<ReservedProduct> reserve(List<ProductReservationReq> requests) {
        ReserveContext reserveContext = ReserveContext.from(requests);
        return productCache.reserve(reserveContext);
    }

    public void restore(List<ProductRestoreReq> requests) {
        productCache.restore(requests);
    }

    public record ProductReservationReq(
            Long productId,
            Integer quantity
    ) {
    }

    public record ReserveContext(
            Map<Long, ReserveProduct> products
    ) {
        private static ReserveContext from(List<ProductReservationReq> requests) {
            Map<Long, ReserveProduct> products = requests.stream()
                    .map(ReserveProduct::from)
                    .collect(Collectors.toMap(
                            ReserveProduct::productId,
                            Function.identity(),
                            ReserveProduct::addQuantity
                    ));
            return new ReserveContext(products);
        }

        public ReserveContext {
            products = Map.copyOf(products);
        }

        public boolean isEmpty() {
            return products.isEmpty();
        }

        public List<Long> productIds() {
            return products.keySet().stream()
                    .sorted()
                    .toList();
        }

        public List<ReserveProduct> reserveProducts() {
            return productIds().stream()
                    .map(products::get)
                    .toList();
        }

        public Integer quantity(Long productId) {
            return products.get(productId).quantity();
        }
    }

    public record ReserveProduct(
            Long productId,
            Integer quantity
    ) {
        private static ReserveProduct from(ProductReservationReq request) {
            return new ReserveProduct(request.productId(), request.quantity());
        }

        private ReserveProduct addQuantity(ReserveProduct product) {
            return new ReserveProduct(productId, quantity + product.quantity());
        }
    }

    public record ProductRestoreReq(
            Long productId,
            Integer quantity
    ) {
    }
}
