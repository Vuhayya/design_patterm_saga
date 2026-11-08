package com.sagademo.saga.dto;

/**
 * One line of the dashboard "Services" panel:
 *
 * <pre>
 *   Order Service       * ONLINE   localhost:8081
 *   Inventory Service   * ONLINE   localhost:8082
 *   Payment Service     * ONLINE   localhost:8083
 *   Saga Orchestrator   * ONLINE   localhost:8084
 * </pre>
 *
 * <p>The status is measured by really calling each service's {@code /health} endpoint, so
 * stopping a service in the terminal turns its indicator red in the browser.
 */
public record ServiceHealthDto(
        String service,
        String displayName,
        String baseUrl,
        String status,
        Long responseTimeMs,
        String database,
        Object simulation,
        String error
) {
}
