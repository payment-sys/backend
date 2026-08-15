package com.v_payment.pay.product.cache;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class LockManager {
    private final Map<Long, ReentrantLock> lockCache = new ConcurrentHashMap<>();

    public List<ReentrantLock> lock(List<Long> lockTargetIds) {
        List<ReentrantLock> targetLocks = lockTargetIds.stream()
                .distinct()
                .sorted()
                .map(id -> lockCache.computeIfAbsent(id, k -> new ReentrantLock()))
                .toList();

        targetLocks.forEach(ReentrantLock::lock);
        return targetLocks;
    }

    public void unlock(List<ReentrantLock> locks) {
        locks.forEach(ReentrantLock::unlock);
    }
}
