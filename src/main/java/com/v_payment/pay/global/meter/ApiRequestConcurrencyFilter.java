package com.v_payment.pay.global.meter;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ApiRequestConcurrencyFilter extends OncePerRequestFilter {
    private final AtomicInteger activeRequests = new AtomicInteger();
    private final AtomicInteger maxActiveRequests = new AtomicInteger();
    private final DistributionSummary activeRequestPeakSummary;

    public ApiRequestConcurrencyFilter(MeterRegistry meterRegistry) {
        this.activeRequestPeakSummary = DistributionSummary.builder("pay.api.requests.active.peak")
                .description("High-water mark of concurrently processing API requests observed on this server")
                .register(meterRegistry);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        AtomicBoolean completed = new AtomicBoolean(false);
        boolean asyncStarted = false;
        recordRequestStarted();

        try {
            filterChain.doFilter(request, response);

            if (request.isAsyncStarted()) {
                request.getAsyncContext().addListener(new RequestCompleteListener(completed));
                asyncStarted = true;
            }
        } finally {
            if (!asyncStarted) {
                recordRequestCompleted(completed);
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

    private void recordRequestCompleted(AtomicBoolean completed) {
        if (completed.compareAndSet(false, true)) {
            activeRequests.decrementAndGet();
        }
    }

    private class RequestCompleteListener implements AsyncListener {
        private final AtomicBoolean completed;

        private RequestCompleteListener(AtomicBoolean completed) {
            this.completed = completed;
        }

        @Override
        public void onComplete(AsyncEvent event) {
            recordRequestCompleted(completed);
        }

        @Override
        public void onTimeout(AsyncEvent event) {
            recordRequestCompleted(completed);
        }

        @Override
        public void onError(AsyncEvent event) {
            recordRequestCompleted(completed);
        }

        @Override
        public void onStartAsync(AsyncEvent event) {
            event.getAsyncContext().addListener(this);
        }
    }
}
