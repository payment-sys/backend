package com.v_payment.pay.product.cache;

import com.v_payment.pay.global.exception.BusinessException;
import com.v_payment.pay.product.cache.wal.WriteAheadLog;
import com.v_payment.pay.product.entity.Product;
import com.v_payment.pay.product.exception.ProductException;
import com.v_payment.pay.product.repository.ProductRepository;
import com.v_payment.pay.product.service.ProductManager;
import com.v_payment.pay.product.service.ProductManager.ReserveContext;
import com.v_payment.pay.product.service.ReservedProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
    public List<ReservedProduct> reserve(ReserveContext reserveContext) {
        List<ReentrantLock> acquiredLocks = lockAll(reserveContext.productIds());
        try {
            Map<Long, CachedProduct> cachedProducts = loadCache(reserveContext);

            writeAheadLog.reserve(reserveContext.reserveProducts());

            subtractStock(reserveContext);

            return createReservedProducts(reserveContext, cachedProducts);
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

            int updatedRows = productRepository.changeStock(product.productId(), product.stockQuantity());
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

    private Map<Long, CachedProduct> loadCache(ReserveContext reserveContext) {
        // 현재 캐시 수량으로 요청을 감당할 수 없는 상품만 DB에서 가져온다.
        List<Long> forLoadIds = reserveContext.productIds().stream()
                .filter(productId -> isInsufficientStock(products.get(productId), reserveContext.quantity(productId)))
                .toList();

        if (!forLoadIds.isEmpty()) {
            // 동시 예약으로 재고가 초과 차감되지 않도록 할당량 계산 전에 row lock을 잡는다.
            Map<Long, Product> productsForLoad = productRepository.findAllByIdInForUpdate(forLoadIds).stream()
                    .collect(Collectors.toMap(Product::getId, Function.identity()));
            if (productsForLoad.size() != forLoadIds.size()) throw new BusinessException(ProductException.PRODUCT_NOT_FOUND);

            // 중간 품절 시 메모리 캐시가 앞서가지 않도록 DB/cache 변경 전에 모든 할당량을 계산한다.
            Map<Long, Integer> allocationQuantityByProductId = new HashMap<>();
            for (Long productId : forLoadIds) {
                CachedProduct cachedProduct = products.get(productId);
                Product loadedProduct = productsForLoad.get(productId);
                int requestedQuantity = reserveContext.quantity(productId);
                int cachedQuantity = cachedProduct == null ? 0 : cachedProduct.stockQuantity();
                int allocationQuantity = allocationQuantity(loadedProduct, requestedQuantity - cachedQuantity);

                if (cachedQuantity + allocationQuantity < requestedQuantity) {
                    throw new BusinessException(ProductException.OUT_OF_STOCK);
                }
                allocationQuantityByProductId.put(productId, allocationQuantity);
            }

            // DB 재고에서 할당 수량만큼 차감해 메모리 캐시로 옮긴다.
            for (Long productId : forLoadIds) {
                int allocationQuantity = allocationQuantityByProductId.get(productId);
                int updatedRows = productRepository.changeStock(productId, -allocationQuantity);
                if (updatedRows != 1) throw new BusinessException(ProductException.OUT_OF_STOCK);
            }

            LocalDateTime lastModifiedAt = LocalDateTime.now(clock);
            forLoadIds.forEach(productId -> {
                CachedProduct cachedProduct = products.get(productId);
                Product loadedProduct = productsForLoad.get(productId);
                int allocationQuantity = allocationQuantityByProductId.get(productId);
                products.put(
                        productId,
                        CachedProduct.from(loadedProduct, stockQuantity(cachedProduct) + allocationQuantity, lastModifiedAt)
                );
            });
        }

        // 요청 수량을 만족하는지 검증된 예약용 캐시 스냅샷을 반환한다.
        Map<Long, CachedProduct> cachedProducts = productIds.stream()
                .collect(Collectors.toMap(Function.identity(), products::get));
        validateStock(reserveContext, cachedProducts);
        return cachedProducts;
    }

    private int allocationQuantity(Product product, int requiredQuantity) {
        if (requiredQuantity <= 0) return 0;
        if (product.getStockQuantity() <= 0) throw new BusinessException(ProductException.OUT_OF_STOCK);
        return Math.min(product.getStockQuantity(), Math.max(CACHE_ALLOCATION_QUANTITY, requiredQuantity));
    }

    private int stockQuantity(CachedProduct product) {
        return product == null ? 0 : product.stockQuantity();
    }

    private boolean isInsufficientStock(CachedProduct product, Integer quantity) {
        return product == null || product.stockQuantity() < quantity;
    }

    private void validateStock(ReserveContext reserveContext, Map<Long, CachedProduct> cachedProducts) {
        reserveContext.reserveProducts().forEach(reserveProduct -> {
            CachedProduct product = cachedProducts.get(reserveProduct.productId());
            if (product == null) throw new BusinessException(ProductException.PRODUCT_NOT_FOUND);
            if (product.stockQuantity() < reserveProduct.quantity()) throw new BusinessException(ProductException.OUT_OF_STOCK);
        });
    }

    private void subtractStock(ReserveContext reserveContext) {
        LocalDateTime lastModifiedAt = LocalDateTime.now(clock);
        reserveContext.reserveProducts()
                .forEach(reserveProduct -> {
                    products.computeIfPresent(
                            reserveProduct.productId(),
                            (k, product) -> product.subtractQuantity(reserveProduct.quantity(), lastModifiedAt)
                    );
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

    private List<ReservedProduct> createReservedProducts(
            ReserveContext reserveContext,
            Map<Long, CachedProduct> cachedProducts
    ) {
        return reserveContext.reserveProducts().stream()
                .map(reserveProduct -> {
                    CachedProduct product = cachedProducts.get(reserveProduct.productId());
                    return new ReservedProduct(product.productId(), product.name(), product.price(), reserveProduct.quantity());
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
