package com.sagademo.payment.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Copies {@code X-Saga-Id} / {@code X-Order-Id} / {@code X-Saga-Step} into the SLF4J MDC so
 * every log line of this service is tagged with the distributed transaction it belongs to.
 */
@Component
@Order(1)
public class SagaTraceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            put("sagaId", request.getHeader("X-Saga-Id"));
            put("orderId", request.getHeader("X-Order-Id"));
            put("step", request.getHeader("X-Saga-Step"));
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private void put(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }
}
