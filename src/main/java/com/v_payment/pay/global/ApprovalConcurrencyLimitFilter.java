package com.v_payment.pay.global;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApprovalConcurrencyLimitFilter extends OncePerRequestFilter {
    private static final String APPROVAL_PATH = "/payments/approvals";
    private final Semaphore activeSemaphore;
    private final Semaphore waitingSemaphore;
    private final Duration maxWaitTime;

    public ApprovalConcurrencyLimitFilter(
            @Value("${payment.approval.concurrency.min:0}") int minConcurrentRequests,
            @Value("${payment.approval.concurrency.max-wait:0}") int maxWaitingRequests,
            @Value("${payment.approval.concurrency.max-wait-time:100ms}") Duration maxWaitTime,
            @Value("${payment.approval.concurrency.max:24}") int maxConcurrentRequests
    ) {
        if (maxConcurrentRequests < 1) {
            throw new IllegalArgumentException("payment.approval.concurrency.max must be greater than 0");
        }
        if (minConcurrentRequests < 0) {
            throw new IllegalArgumentException("payment.approval.concurrency.min must not be negative");
        }
        if (minConcurrentRequests >= maxConcurrentRequests) {
            throw new IllegalArgumentException("payment.approval.concurrency.min must be less than max");
        }
        if (maxWaitingRequests < 0) {
            throw new IllegalArgumentException("payment.approval.concurrency.max-wait must not be negative");
        }
        if (maxWaitTime.isNegative()) {
            throw new IllegalArgumentException("payment.approval.concurrency.max-wait-time must not be negative");
        }
        this.activeSemaphore = new Semaphore(maxConcurrentRequests, true);
        this.waitingSemaphore = new Semaphore(maxWaitingRequests, true);
        this.maxWaitTime = maxWaitTime;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !APPROVAL_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        boolean acquired;
        try {
            acquired = tryAcquireExecutionPermit();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            return;
        }

        if (!acquired) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            activeSemaphore.release();
        }
    }

    private boolean tryAcquireExecutionPermit() throws InterruptedException {
        if (activeSemaphore.tryAcquire(0, TimeUnit.MILLISECONDS)) {
            return true;
        }
        if (!waitingSemaphore.tryAcquire()) {
            return false;
        }

        try {
            return activeSemaphore.tryAcquire(maxWaitTime.toNanos(), TimeUnit.NANOSECONDS);
        } finally {
            waitingSemaphore.release();
        }
    }
}
