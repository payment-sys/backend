package com.v_payment.pay.log;

import com.v_payment.pay.global.LTimer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.v_payment.pay.log.ApiLogContext.ELAPSED_MS;
import static com.v_payment.pay.log.ApiLogContext.METHOD;
import static com.v_payment.pay.log.ApiLogContext.PATH;
import static com.v_payment.pay.log.ApiLogContext.TRACE_ID;

@Slf4j(topic = "API_LOGGER")
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ApiLogContext apiLogContext = ApiLogContext.create(request);

        long start = LTimer.getCurrTime();
        try {
            MDC.put(TRACE_ID, apiLogContext.traceId());
            MDC.put(METHOD, apiLogContext.method());
            MDC.put(PATH, apiLogContext.path());

            log.info("request start");
            filterChain.doFilter(request, response);
        } finally {
            MDC.put(ELAPSED_MS, String.valueOf(LTimer.getDiff(start)));
            log.info("request end status = {}", response.getStatus());

            MDC.remove(TRACE_ID);
            MDC.remove(METHOD);
            MDC.remove(PATH);
            MDC.remove(ELAPSED_MS);
        }
    }
}
