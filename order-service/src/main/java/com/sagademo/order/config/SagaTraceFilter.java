package com.sagademo.order.config;

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
 * DISTRIBUTED TRACING, the simple way (requirement #17).
 *
 * <p>The Saga Orchestrator sends two headers with every call:
 * {@code X-Saga-Id} and {@code X-Order-Id}. This filter copies them into the SLF4J MDC so
 * that <b>every</b> log line produced while handling the request is automatically tagged:
 *
 * <pre>
 *   10:31:04.118 INFO  [ORDER_SERVICE] [saga=SAGA-20260926-0003] [order=ORDER-0007] ...
 * </pre>
 *
 * That is what makes it possible to follow ONE distributed transaction across five
 * independent service log windows during the presentation.
 */
@Component
@Order(1)
public class SagaTraceFilter extends OncePerRequestFilter {

    public static final String SAGA_ID_HEADER = "X-Saga-Id";
    public static final String ORDER_ID_HEADER = "X-Order-Id";
    public static final String STEP_HEADER = "X-Saga-Step";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            put("sagaId", request.getHeader(SAGA_ID_HEADER));
            put("orderId", request.getHeader(ORDER_ID_HEADER));
            put("step", request.getHeader(STEP_HEADER));
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
