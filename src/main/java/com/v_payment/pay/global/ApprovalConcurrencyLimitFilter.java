package com.v_payment.pay.global;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
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
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ApprovalConcurrencyLimitFilter extends OncePerRequestFilter {
    private static final String APPROVAL_PATH = "/payments/approvals";
    private final Semaphore activeSemaphore;
    private final Semaphore waitingSemaphore;
    private final Duration maxWaitTime;
    private final int minConcurrentRequests;
    private final int maxConcurrentRequests;
    private final int maxWaitingRequests;
    private final Counter waitingQueueFullCounter;
    private final Counter waitTimeoutCounter;
    private final Counter interruptedCounter;
    private final DistributionSummary waitingDepthSummary;

    public ApprovalConcurrencyLimitFilter(
            @Value("${payment.approval.concurrency.min:0}") int minConcurrentRequests,
            @Value("${payment.approval.concurrency.max-wait:0}") int maxWaitingRequests,
            @Value("${payment.approval.concurrency.max-wait-time:100ms}") Duration maxWaitTime,
            @Value("${payment.approval.concurrency.max:24}") int maxConcurrentRequests,
            MeterRegistry meterRegistry
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
        this.minConcurrentRequests = minConcurrentRequests;
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.maxWaitingRequests = maxWaitingRequests;
        this.waitingQueueFullCounter = rejectedCounter(meterRegistry, "waiting_queue_full");
        this.waitTimeoutCounter = rejectedCounter(meterRegistry, "wait_timeout");
        this.interruptedCounter = rejectedCounter(meterRegistry, "interrupted");
        this.waitingDepthSummary = DistributionSummary.builder("payment_approval_concurrency_waiting_depth")
                .description("Observed approval waiting queue depth")
                .baseUnit("requests")
                .register(meterRegistry);
        registerGauges(meterRegistry);
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
            interruptedCounter.increment();
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
            recordWaitingDepth();
            return AcquireResult.ACQUIRED;
        }
        if (!waitingSemaphore.tryAcquire()) {
            recordWaitingDepth(maxWaitingRequests);
            return AcquireResult.WAITING_QUEUE_FULL;
        }

        recordWaitingDepth();
        try {
            if (activeSemaphore.tryAcquire(maxWaitTime.toNanos(), TimeUnit.NANOSECONDS)) {
                return AcquireResult.ACQUIRED;
            }
            return AcquireResult.WAIT_TIMEOUT;
        } finally {
            waitingSemaphore.release();
            recordWaitingDepth();
        }
    }

    private void recordRejection(AcquireResult acquireResult) {
        if (acquireResult == AcquireResult.WAITING_QUEUE_FULL) {
            waitingQueueFullCounter.increment();
            return;
        }
        if (acquireResult == AcquireResult.WAIT_TIMEOUT) {
            waitTimeoutCounter.increment();
        }
    }

    private Counter rejectedCounter(MeterRegistry meterRegistry, String reason) {
        return Counter.builder("payment_approval_concurrency_rejected")
                .description("Total approval requests rejected by the concurrency limit filter")
                .tag("reason", reason)
                .register(meterRegistry);
    }

    private void registerGauges(MeterRegistry meterRegistry) {
        Gauge.builder("payment_approval_concurrency_active_requests", this, ApprovalConcurrencyLimitFilter::activeRequests)
                .description("Current approval requests executing past the concurrency limit filter")
                .register(meterRegistry);
        Gauge.builder("payment_approval_concurrency_active_available", this, ApprovalConcurrencyLimitFilter::activeAvailable)
                .description("Current available approval execution slots")
                .register(meterRegistry);
        Gauge.builder("payment_approval_concurrency_waiting_requests", this, ApprovalConcurrencyLimitFilter::waitingRequests)
                .description("Current approval requests waiting for an execution slot")
                .register(meterRegistry);
        Gauge.builder("payment_approval_concurrency_waiting_available", this, ApprovalConcurrencyLimitFilter::waitingAvailable)
                .description("Current available approval waiting slots")
                .register(meterRegistry);
        Gauge.builder("payment_approval_concurrency_max_requests", this, filter -> filter.maxConcurrentRequests)
                .description("Configured max approval execution slots")
                .register(meterRegistry);
        Gauge.builder("payment_approval_concurrency_min_requests", this, filter -> filter.minConcurrentRequests)
                .description("Configured min approval concurrency value")
                .register(meterRegistry);
        Gauge.builder("payment_approval_concurrency_max_wait_requests", this, filter -> filter.maxWaitingRequests)
                .description("Configured max approval waiting slots")
                .register(meterRegistry);
        Gauge.builder("payment_approval_concurrency_max_wait_time_ms", this, filter -> filter.maxWaitTime.toMillis())
                .description("Configured max time an approval request can wait for an execution slot")
                .register(meterRegistry);
    }

    private double activeRequests() {
        return maxConcurrentRequests - activeSemaphore.availablePermits();
    }

    private double activeAvailable() {
        return activeSemaphore.availablePermits();
    }

    private double waitingRequests() {
        return maxWaitingRequests - waitingSemaphore.availablePermits();
    }

    private double waitingAvailable() {
        return waitingSemaphore.availablePermits();
    }

    private void recordWaitingDepth() {
        recordWaitingDepth((int) waitingRequests());
    }

    private void recordWaitingDepth(int waiting) {
        waitingDepthSummary.record(Math.max(waiting, 0));
    }

    private enum AcquireResult {
        ACQUIRED,
        WAITING_QUEUE_FULL,
        WAIT_TIMEOUT
    }
}
