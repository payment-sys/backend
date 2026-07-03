package com.v_payment.pay.payment.payment.limiter;

import com.v_payment.pay.global.meter.CounterMeter;
import com.v_payment.pay.global.meter.DistributionSummaryMeter;
import com.v_payment.pay.payment.payment.config.PaymentApprovalConcurrencyProperties;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ApprovalConcurrencyLimitFilter extends OncePerRequestFilter {
    private static final String APPROVAL_PATH = "/payments/approvals";

    private final PaymentApprovalConcurrencyProperties properties;
    private final CounterMeter requestRejectFullCounter;
    private final CounterMeter requestRejectTimeoutCounter;
    private final CounterMeter requestRejectInterruptCounter;
    private final DistributionSummaryMeter requestRunSummary;
    private final DistributionSummaryMeter requestWaitSummary;

    private Semaphore activeSemaphore;
    private Semaphore waitingSemaphore;
    private Duration maxWaitTime;
    private int minConcurrentRequests;
    private int maxConcurrentRequests;
    private int maxWaitingRequests;

    @PostConstruct
    void init() {
        this.minConcurrentRequests = properties.min();
        this.maxConcurrentRequests = properties.max();
        this.maxWaitingRequests = properties.maxWait();
        this.maxWaitTime = properties.maxWaitTime();
        this.activeSemaphore = new Semaphore(maxConcurrentRequests, true);
        this.waitingSemaphore = new Semaphore(maxWaitingRequests, true);
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
        AcquireResult acquireResult;
        try {
            acquireResult = tryAcquireExecutionPermit();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            requestRejectInterruptCounter.increment();
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            return;
        }

        if (acquireResult != AcquireResult.ACQUIRED) {
            recordRejection(acquireResult);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            activeSemaphore.release();
        }
    }

    private AcquireResult tryAcquireExecutionPermit() throws InterruptedException {
        if (activeSemaphore.tryAcquire(0, TimeUnit.MILLISECONDS)) {
            recordRun();
            recordWait();
            return AcquireResult.ACQUIRED;
        }
        if (!waitingSemaphore.tryAcquire()) {
            recordWait(maxWaitingRequests);
            return AcquireResult.WAITING_QUEUE_FULL;
        }

        recordWait();
        try {
            if (activeSemaphore.tryAcquire(maxWaitTime.toNanos(), TimeUnit.NANOSECONDS)) {
                recordRun();
                return AcquireResult.ACQUIRED;
            }
            return AcquireResult.WAIT_TIMEOUT;
        } finally {
            waitingSemaphore.release();
            recordWait();
        }
    }

    private void recordRejection(AcquireResult acquireResult) {
        if (acquireResult == AcquireResult.WAITING_QUEUE_FULL) {
            requestRejectFullCounter.increment();
            return;
        }
        if (acquireResult == AcquireResult.WAIT_TIMEOUT) {
            requestRejectTimeoutCounter.increment();
        }
    }

    public double activeRequests() {
        return maxConcurrentRequests - activeSemaphore.availablePermits();
    }

    public double activeAvailable() {
        return activeSemaphore.availablePermits();
    }

    public double waitingRequests() {
        return maxWaitingRequests - waitingSemaphore.availablePermits();
    }

    public double waitingAvailable() {
        return waitingSemaphore.availablePermits();
    }

    public double maxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    public double minConcurrentRequests() {
        return minConcurrentRequests;
    }

    public double maxWaitingRequests() {
        return maxWaitingRequests;
    }

    public double maxWaitTimeMillis() {
        return maxWaitTime.toMillis();
    }

    private void recordWait() {
        recordWait((int) waitingRequests());
    }

    private void recordWait(int waiting) {
        requestWaitSummary.record(Math.max(waiting, 0));
    }

    private void recordRun() {
        requestRunSummary.record((int) activeRequests());
    }

    private enum AcquireResult {
        ACQUIRED,
        WAITING_QUEUE_FULL,
        WAIT_TIMEOUT
    }
}
