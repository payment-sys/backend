package com.v_payment.pay.product.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCacheRestoreScheduler {
    private static final Duration STALE_AFTER = Duration.ofMinutes(3);

    private final ProductCache productCache;

    @Scheduled(fixedDelayString = "${product.cache.restore.fixed-delay-ms:30000}")
    public void restoreStaleProducts() {
        int restoredCount = productCache.restoreStaleProducts(STALE_AFTER);
        if (restoredCount > 0) {
            log.info("Restored stale product cache entries. count={}", restoredCount);
        }
    }
}
