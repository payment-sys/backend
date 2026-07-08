package com.v_payment.pay.log;

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

import static com.v_payment.pay.log.ApiLogContext.LOG_TYPE;
import static com.v_payment.pay.log.ApiLogContext.METHOD;
import static com.v_payment.pay.log.ApiLogContext.PATH;

@Slf4j(topic = "API_LOGGER")
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ApiLogContext apiLogContext = ApiLogContext.create(request);

        try {
            MDC.put(LOG_TYPE, "api");
            MDC.put(METHOD, apiLogContext.method());
            MDC.put(PATH, apiLogContext.path());

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(LOG_TYPE);
            MDC.remove(METHOD);
            MDC.remove(PATH);
        }
    }
}
