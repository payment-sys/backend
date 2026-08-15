package com.v_payment.pay.product.service;

import com.v_payment.pay.global.exception.BusinessException;
import com.v_payment.pay.product.cache.CacheLoadPolicy;
import com.v_payment.pay.product.cache.LockManager;
import com.v_payment.pay.product.cache.ProductCache;
import com.v_payment.pay.product.cache.ProductReservationWriteAheadLog;
import com.v_payment.pay.product.cache.dto.CachedProduct;
import com.v_payment.pay.product.controller.dto.res.ReservedProduct;
import com.v_payment.pay.product.entity.Product;
import com.v_payment.pay.product.exception.ProductException;
import com.v_payment.pay.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductManager {
    private final ProductCache productCache;
    private final LockManager lockManager;
    private final CacheLoadPolicy cacheLoadPolicy;
    private final ProductRepository productRepository;
    private final ProductReservationWriteAheadLog writeAheadLog;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ReservedProduct> reserve(List<ProductReservationReq> reqs) {
        if (reqs.isEmpty()) return List.of();

        List<Long> productIds = reqs.stream().map(req -> req.productId).distinct().sorted().toList();
        Map<Long, Integer> reqsMap = reqs.stream().collect(
                Collectors.toMap(req -> req.productId, req -> req.quantity)
        );

        return lockManager.withLock(productIds, () -> {
            Map<Long, CachedProduct> productsInCache = productCache.findAll(productIds);

            Map<Long, Product> productsForLoad = findProductInDbIfNeeded(productsInCache, reqsMap);

            List<ReservationPlan> reservationPlans = planProductReservations(productsForLoad, productsInCache, reqsMap);

            appendReservationDeltaLog(reservationPlans, reqsMap);

            List<CachedProduct> cachedProducts = applyReservationPlans(reservationPlans);

            return cachedProducts.stream()
                    .map(cachedProduct -> ReservedProduct.of(cachedProduct.productId(),
                            cachedProduct.productName(),cachedProduct.price(), cachedProduct.quantity()))
                    .toList();
        });
    }

    public void restore(List<ProductRestoreReq> reqs) {
        if (reqs.isEmpty()) return;

        List<Long> productIds = reqs.stream().map(req -> req.productId).distinct().sorted().toList();
        Map<Long, Integer> reqsMap = reqs.stream().collect(
                Collectors.toMap(req -> req.productId, req -> req.quantity)
        );

        lockManager.withLock(productIds, () -> {
            Map<Long, CachedProduct> productsInCache = productCache.findAll(productIds);

            Map<Long, Product> productsForLoad = findProductInDbForRestoreIfNeeded(productsInCache, productIds);

            List<RestorePlan> restorePlans = planProductRestores(productsForLoad, productsInCache, reqsMap);

            writeAheadLog.append(reqsMap);

            restoreProducts(restorePlans, productsInCache);

            return null;
        });
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreReservedProductsOnOrderCreationFailure(List<ProductReservationReq> reqs) {
        if (reqs.isEmpty()) return;

        List<ProductRestoreReq> restoreReqs = reqs.stream()
                .map(req -> new ProductRestoreReq(req.productId(), req.quantity()))
                .toList();

        restore(restoreReqs);
    }

    private List<ReservationPlan> planProductReservations(Map<Long, Product> productsForLoad, Map<Long, CachedProduct> productsInCache, Map<Long, Integer> reqsMap) {
        return reqsMap.entrySet().stream().map(entry -> {
                    Long productId = entry.getKey();
                    int requestedQuantity = entry.getValue();
                    CachedProduct cachedProduct = productsInCache.get(productId);

                    if (isEnoughQuantityInCache(cachedProduct, requestedQuantity)) {
                        CachedProduct reservedProduct = cachedProduct.changeQuantity(-requestedQuantity);
                        return new ReservationPlan(productId, 0, null, reservedProduct);
                    }

                    Product productForReserve = productsForLoad.get(productId);
                    if (productForReserve == null) throw new BusinessException(ProductException.PRODUCT_NOT_FOUND);

                    if (isNotEnoughInDBAndCache(requestedQuantity, productForReserve, cachedProduct)) {
                        throw new BusinessException(ProductException.OUT_OF_STOCK);
                    }

                    int loadQuantityAtDb = cacheLoadPolicy.getLoadQuantityCount(
                            getCachedQuantity(cachedProduct), productForReserve.getStockQuantity(), requestedQuantity);

                    CachedProduct reservedProduct = reserveLoadedProduct(productForReserve, cachedProduct, loadQuantityAtDb, requestedQuantity);
                    return new ReservationPlan(productId, loadQuantityAtDb, productForReserve, reservedProduct);
                })
                .toList();
    }

    private void appendReservationDeltaLog(List<ReservationPlan> reservationPlans, Map<Long, Integer> reqsMap) {
        Map<Long, Integer> cacheDeltasByProductId = reservationPlans.stream()
                .filter(plan -> plan.loadQuantity() - reqsMap.get(plan.productId()) != 0)
                .collect(Collectors.toMap(
                        ReservationPlan::productId,
                        plan -> plan.loadQuantity() - reqsMap.get(plan.productId())
                ));

        if (!cacheDeltasByProductId.isEmpty()) {
            writeAheadLog.append(cacheDeltasByProductId);
        }
    }

    private List<CachedProduct> applyReservationPlans(List<ReservationPlan> reservationPlans) {
        return reservationPlans.stream()
                .map(plan -> {
                    if (plan.loadQuantity() > 0) {
                        plan.productForReserve().subtractQuantity(plan.loadQuantity());
                    }
                    productCache.put(plan.productId(), plan.reservedProduct());
                    return plan.reservedProduct();
                })
                .toList();
    }

    private boolean isNotEnoughInDBAndCache(Integer requestedQuantity, Product productForReserve, CachedProduct cachedProduct) {
        return productForReserve.getStockQuantity() + getCachedQuantity(cachedProduct) < requestedQuantity;
    }

    private boolean isEnoughQuantityInCache(CachedProduct cachedProduct, Integer requestedQuantity) {
        return cachedProduct != null && cachedProduct.quantity() >= requestedQuantity;
    }

    private CachedProduct reserveLoadedProduct(Product productForReserve, CachedProduct cachedProduct, int loadedQuantity, int requestedQuantity) {
        int reservedQuantity = getCachedQuantity(cachedProduct) + loadedQuantity - requestedQuantity;
        return new CachedProduct(
                productForReserve.getId(),
                productForReserve.getName(),
                productForReserve.getPrice(),
                reservedQuantity
        );
    }

    private int getCachedQuantity(CachedProduct cachedProduct) {
        return cachedProduct == null ? 0 : cachedProduct.quantity();
    }

    private Map<Long, Product> findProductInDbIfNeeded(Map<Long, CachedProduct> productsInCache, Map<Long, Integer> reqsMap) {
        List<Long> productIdsForFind = reqsMap.keySet().stream()
                .filter(productId -> isNotCached(productsInCache, productId) || needMoreQuantity(productsInCache, reqsMap, productId))
                .sorted()
                .toList();

        if (productIdsForFind.isEmpty()) return Map.of();
        return productRepository.findAllByIdInForUpdate(productIdsForFind).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private boolean isNotCached(Map<Long, CachedProduct> productsInCache, Long productId) {
        return !productsInCache.containsKey(productId);
    }

    private boolean needMoreQuantity(Map<Long, CachedProduct> productsInCache, Map<Long, Integer> reqsMap, Long productId) {
        return productsInCache.get(productId).quantity() < reqsMap.get(productId);
    }

    private Map<Long, Product> findProductInDbForRestoreIfNeeded(Map<Long, CachedProduct> productsInCache, List<Long> productIds) {
        List<Long> productIdsForFind = productIds.stream()
                .filter(productId -> !productsInCache.containsKey(productId))
                .toList();

        if (productIdsForFind.isEmpty()) return Map.of();
        return productRepository.findAllByIdInForUpdate(productIdsForFind).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private List<RestorePlan> planProductRestores(Map<Long, Product> productsForLoad, Map<Long, CachedProduct> productsInCache, Map<Long, Integer> reqsMap) {
        return reqsMap.entrySet().stream()
                .map(entry -> new RestorePlan(
                        entry.getKey(),
                        restoreProduct(entry.getKey(), entry.getValue(), productsInCache, productsForLoad)
                ))
                .toList();
    }

    private void restoreProducts(List<RestorePlan> restorePlans, Map<Long, CachedProduct> productsInCache) {
        restorePlans.forEach(plan -> {
            productCache.put(plan.productId(), plan.restoredProduct());
            productsInCache.put(plan.productId(), plan.restoredProduct());
        });
    }

    private CachedProduct restoreProduct(
            Long productId,
            Integer quantity,
            Map<Long, CachedProduct> productsInCache,
            Map<Long, Product> productsForLoad
    ) {
        CachedProduct cachedProduct = productsInCache.get(productId);
        if (cachedProduct != null) {
            return cachedProduct.changeQuantity(quantity);
        }

        Product product = productsForLoad.get(productId);
        if (product == null) throw new BusinessException(ProductException.PRODUCT_NOT_FOUND);
        return new CachedProduct(product.getId(), product.getName(), product.getPrice(), quantity);
    }

    public record ProductReservationReq(
            Long productId,
            Integer quantity
    ) {
    }

    public record ProductRestoreReq(
            Long productId,
            Integer quantity
    ) {
    }

    private record ReservationPlan(
            Long productId,
            int loadQuantity,
            Product productForReserve,
            CachedProduct reservedProduct
    ) {
    }

    private record RestorePlan(
            Long productId,
            CachedProduct restoredProduct
    ) {
    }
}
