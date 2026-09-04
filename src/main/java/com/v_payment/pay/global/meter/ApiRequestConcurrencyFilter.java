package com.v_payment.pay.global.meter;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ApiRequestConcurrencyFilter extends OncePerRequestFilter {
    private final AtomicInteger activeRequests = new AtomicInteger();
    private final AtomicInteger maxActiveRequests = new AtomicInteger();
    private final MeterRegistry meterRegistry;
    private final DistributionSummary activeRequestPeakSummary;

    public ApiRequestConcurrencyFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.activeRequestPeakSummary = DistributionSummary.builder("pay.api.requests.active.peak")
                .description("High-water mark of concurrently processing API requests observed on this server")
                .register(meterRegistry);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        AtomicBoolean completed = new AtomicBoolean(false);
        boolean asyncStarted = false;
        long startedAtNanos = System.nanoTime();
        recordRequestStarted();

        try {
            filterChain.doFilter(request, response);

            if (request.isAsyncStarted()) {
                request.getAsyncContext().addListener(
                        new RequestCompleteListener(request, response, completed, startedAtNanos)
                );
                asyncStarted = true;
            }
        } finally {
            if (!asyncStarted) {
                recordRequestCompleted(request, response, completed, startedAtNanos);
            }
        }
    }

    private void recordRequestStarted() {
        int active = activeRequests.incrementAndGet();
        int previousMax = maxActiveRequests.getAndAccumulate(active, Math::max);
        if (active > previousMax) {
            activeRequestPeakSummary.record(active);
        }
    }

    private void recordRequestCompleted(
            HttpServletRequest request,
            HttpServletResponse response,
            AtomicBoolean completed,
            long startedAtNanos
    ) {
        if (completed.compareAndSet(false, true)) {
            activeRequests.decrementAndGet();
            recordRequestDuration(request, response, startedAtNanos);
        }
    }

    private void recordRequestDuration(HttpServletRequest request, HttpServletResponse response, long startedAtNanos) {
        Timer.builder("pay.api.requests.duration")
                .description("Pay API request-response duration")
                .tag("method", request.getMethod())
                .tag("uri", uri(request))
                .tag("status", String.valueOf(response.getStatus()))
                .tag("outcome", outcome(response.getStatus()))
                .register(meterRegistry)
                .record(System.nanoTime() - startedAtNanos, TimeUnit.NANOSECONDS);
    }

    private String uri(HttpServletRequest request) {
        Object bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (bestMatchingPattern instanceof String pattern && !pattern.isBlank()) {
            return pattern;
        }
        return "UNKNOWN";
    }

    private String outcome(int status) {
        if (status >= 100 && status < 200) return "INFORMATIONAL";
        if (status >= 200 && status < 300) return "SUCCESS";
        if (status >= 300 && status < 400) return "REDIRECTION";
        if (status >= 400 && status < 500) return "CLIENT_ERROR";
        if (status >= 500) return "SERVER_ERROR";
        return "UNKNOWN";
    }

    private class RequestCompleteListener implements AsyncListener {
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final AtomicBoolean completed;
        private final long startedAtNanos;

        private RequestCompleteListener(
                HttpServletRequest request,
                HttpServletResponse response,
                AtomicBoolean completed,
                long startedAtNanos
        ) {
            this.request = request;
            this.response = response;
            this.completed = completed;
            this.startedAtNanos = startedAtNanos;
        }

        @Override
        public void onComplete(AsyncEvent event) {
            recordRequestCompleted(request, response, completed, startedAtNanos);
        }

        @Override
        public void onTimeout(AsyncEvent event) {
            recordRequestCompleted(request, response, completed, startedAtNanos);
        }

        @Override
        public void onError(AsyncEvent event) {
            recordRequestCompleted(request, response, completed, startedAtNanos);
        }

        @Override
        public void onStartAsync(AsyncEvent event) {
            event.getAsyncContext().addListener(this);
        }
    }
}
