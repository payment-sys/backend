package com.v_payment.pay.product.cache;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Component
public class LockManager {
    private final Map<Long, ReentrantLock> lockCache = new ConcurrentHashMap<>();

    public <T> T withLock(List<Long> lockTargetIds, Supplier<T> supplier) {
        List<ReentrantLock> locks = lock(lockTargetIds);
        try{
            return supplier.get();
        } finally {
            unlock(locks);
        }
    }

    private List<ReentrantLock> lock(List<Long> lockTargetIds) {
        List<ReentrantLock> targetLocks = lockTargetIds.stream()
                .distinct()
                .sorted()
                .map(id -> lockCache.computeIfAbsent(id, k -> new ReentrantLock()))
                .toList();

        targetLocks.forEach(ReentrantLock::lock);
        return targetLocks;
    }

    private void unlock(List<ReentrantLock> locks) {
        locks.forEach(ReentrantLock::unlock);
    }
}
