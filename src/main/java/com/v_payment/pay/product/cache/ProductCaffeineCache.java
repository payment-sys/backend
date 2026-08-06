package com.v_payment.pay.product.cache;

import com.v_payment.pay.global.exception.BusinessException;
import com.v_payment.pay.product.cache.wal.WriteAheadLog;
import com.v_payment.pay.product.entity.Product;
import com.v_payment.pay.product.exception.ProductException;
import com.v_payment.pay.product.repository.ProductRepository;
import com.v_payment.pay.product.service.ProductManager;
import com.v_payment.pay.product.service.ReservedProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductCaffeineCache implements ProductCache {
    private static final int CACHE_ALLOCATION_QUANTITY = 400;

    private final Map<Long, CachedProduct> products = new ConcurrentHashMap<>();
    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final ProductRepository productRepository;
    private final WriteAheadLog writeAheadLog;
    private final Clock clock;

    @Override
    @Transactional
    public List<ReservedProduct> reserve(List<ProductManager.ProductReservationReq> requests) {
        if (requests.isEmpty()) return List.of();

        List<Long> productIds = requests.stream()
                .map(ProductManager.ProductReservationReq::productId)
                .distinct()
                .sorted()
                .toList();

        List<ReentrantLock> acquiredLocks = lockAll(productIds);
        try {
            allocateProductsIfNeeded(productIds);
            validateStock(requests);
            writeAheadLog.reserve(requests);
            subtractStock(requests);
            return createReservedProducts(requests);
        } finally {
            unlockAll(acquiredLocks);
        }
    }

    @Override
    public void restore(List<ProductManager.ProductRestoreReq> requests) {
        if (requests.isEmpty()) return;

        List<Long> productIds = requests.stream()
                .map(ProductManager.ProductRestoreReq::productId)
                .distinct()
                .sorted()
                .toList();
        List<ReentrantLock> acquiredLocks = lockAll(productIds);

        try {
            loadMissingProducts(productIds);
            writeAheadLog.restore(requests);
            addStock(requests);
        } finally {
            unlockAll(acquiredLocks);
        }
    }

    @Override
    @Transactional
    public int restoreStaleProducts(Duration staleAfter) {
        LocalDateTime threshold = LocalDateTime.now(clock).minus(staleAfter);
        List<Long> staleProductIds = products.values().stream()
                .filter(product -> product.stockQuantity() > 0)
                .filter(product -> !product.lastModifiedAt().isAfter(threshold))
                .map(CachedProduct::productId)
                .sorted()
                .toList();

        int restoredCount = 0;
        for (Long productId : staleProductIds) {
            restoredCount += restoreStaleProduct(productId, threshold);
        }
        return restoredCount;
    }

    private int restoreStaleProduct(Long productId, LocalDateTime threshold) {
        ReentrantLock lock = locks.computeIfAbsent(productId, ignored -> new ReentrantLock());
        lock.lock();
        try {
            CachedProduct product = products.get(productId);
            if (product == null || product.stockQuantity() <= 0 || product.lastModifiedAt().isAfter(threshold)) {
                return 0;
            }

            int updatedRows = productRepository.increaseStock(product.productId(), product.stockQuantity());
            if (updatedRows != 1) {
                throw new BusinessException(ProductException.PRODUCT_NOT_FOUND);
            }
            products.remove(product.productId());
            return 1;
        } finally {
            lock.unlock();
        }
    }

    private List<ReentrantLock> lockAll(List<Long> productIds) {
        List<ReentrantLock> acquiredLocks = new ArrayList<>(productIds.size());
        for (Long productId : productIds) {
            ReentrantLock lock = locks.computeIfAbsent(productId, ignored -> new ReentrantLock());
            lock.lock();
            acquiredLocks.add(lock);
        }
        return acquiredLocks;
    }

    private void unlockAll(List<ReentrantLock> acquiredLocks) {
        for (int i = acquiredLocks.size() - 1; i >= 0; i--) {
            acquiredLocks.get(i).unlock();
        }
    }

    private void loadMissingProducts(List<Long> productIds) {
        List<Long> missingProductIds = productIds.stream()
                .filter(productId -> !products.containsKey(productId))
                .toList();
        if (missingProductIds.isEmpty()) return;

        List<Product> loadedProducts = productRepository.findAllById(missingProductIds);
        if (loadedProducts.size() != missingProductIds.size()) {
            throw new BusinessException(ProductException.PRODUCT_NOT_FOUND);
        }

        loadedProducts.stream()
                .map(product -> CachedProduct.from(product, 0, LocalDateTime.now(clock)))
                .forEach(product -> products.put(product.productId(), product));
    }

    private void allocateProductsIfNeeded(List<Long> productIds) {
        List<Long> allocationTargetProductIds = productIds.stream()
                .filter(productId -> {
                    CachedProduct product = products.get(productId);
                    return product == null || product.stockQuantity() == 0;
                })
                .toList();
        if (allocationTargetProductIds.isEmpty()) return;

        Map<Long, Product> loadedProductById = productRepository.findAllById(allocationTargetProductIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        if (loadedProductById.size() != allocationTargetProductIds.size()) {
            throw new BusinessException(ProductException.PRODUCT_NOT_FOUND);
        }

        for (Long productId : allocationTargetProductIds) {
            int updatedRows = productRepository.decreaseStockIfAvailable(productId, CACHE_ALLOCATION_QUANTITY);
            if (updatedRows != 1) {
                throw new BusinessException(ProductException.OUT_OF_STOCK);
            }
        }

        LocalDateTime lastModifiedAt = LocalDateTime.now(clock);
        allocationTargetProductIds.forEach(productId -> {
            Product loadedProduct = loadedProductById.get(productId);
            products.put(productId, CachedProduct.from(loadedProduct, CACHE_ALLOCATION_QUANTITY, lastModifiedAt));
        });
    }

    private void validateStock(List<ProductManager.ProductReservationReq> requests) {
        Map<Long, Integer> quantityByProductId = requests.stream()
                .collect(Collectors.groupingBy(
                        ProductManager.ProductReservationReq::productId,
                        Collectors.summingInt(ProductManager.ProductReservationReq::quantity)
                ));

        quantityByProductId.forEach((productId, quantity) -> {
            CachedProduct product = products.get(productId);
            if (product == null) throw new BusinessException(ProductException.PRODUCT_NOT_FOUND);
            if (product.stockQuantity() < quantity) throw new BusinessException(ProductException.OUT_OF_STOCK);
        });
    }

    private void subtractStock(List<ProductManager.ProductReservationReq> requests) {
        LocalDateTime lastModifiedAt = LocalDateTime.now(clock);
        Map<Long, Integer> quantityByProductId = requests.stream()
                .collect(Collectors.groupingBy(
                        ProductManager.ProductReservationReq::productId,
                        Collectors.summingInt(ProductManager.ProductReservationReq::quantity)
                ));

        quantityByProductId.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    products.computeIfPresent(entry.getKey(), (k, product) -> product.subtractQuantity(entry.getValue(), lastModifiedAt));
                });
    }

    private void addStock(List<ProductManager.ProductRestoreReq> requests) {
        LocalDateTime lastModifiedAt = LocalDateTime.now(clock);
        Map<Long, Integer> quantityByProductId = requests.stream()
                .collect(Collectors.groupingBy(
                        ProductManager.ProductRestoreReq::productId,
                        Collectors.summingInt(ProductManager.ProductRestoreReq::quantity)
                ));

        quantityByProductId.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> products.computeIfPresent(entry.getKey(), (k, product) -> product.addQuantity(entry.getValue(), lastModifiedAt)));
    }

    private List<ReservedProduct> createReservedProducts(List<ProductManager.ProductReservationReq> requests) {
        return requests.stream()
                .map(request -> {
                    CachedProduct product = products.get(request.productId());
                    return new ReservedProduct(product.productId(), product.name(), product.price(), request.quantity());
                })
                .toList();
    }

    private record CachedProduct(
            Long productId,
            String name,
            Long price,
            Integer stockQuantity,
            LocalDateTime lastModifiedAt
    ) {
        private static CachedProduct from(Product product, LocalDateTime lastModifiedAt) {
            return from(product, product.getStockQuantity(), lastModifiedAt);
        }

        private static CachedProduct from(Product product, Integer stockQuantity, LocalDateTime lastModifiedAt) {
            return new CachedProduct(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    stockQuantity,
                    lastModifiedAt
            );
        }

        private CachedProduct subtractQuantity(Integer quantity, LocalDateTime lastModifiedAt) {
            return new CachedProduct(productId, name, price, stockQuantity - quantity, lastModifiedAt);
        }

        private CachedProduct addQuantity(Integer quantity, LocalDateTime lastModifiedAt) {
            return new CachedProduct(productId, name, price, stockQuantity + quantity, lastModifiedAt);
        }
    }
}
