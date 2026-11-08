package com.sagademo.saga.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Everything about the Saga that can be tuned from {@code application.yml} or from an
 * environment variable (requirement #16: all URLs configurable).
 */
@Data
@ConfigurationProperties(prefix = "saga")
public class SagaProperties {

    /**
     * Pause inserted before each step purely for the presentation, so the audience can watch
     * the timeline fill in one box at a time. Set to 0 for tests / fast runs.
     */
    private long stepDelayMs = 700;

    /**
     * How many times a COMPENSATING transaction is attempted before the saga is declared
     * COMPENSATION_FAILED. 1 = no retry. Keeping a small retry count here is deliberate: a real
     * Saga would retry a compensation (compensations must be idempotent), but it must still be
     * able to give up and ask a human - which is exactly what demo Scenario 5 shows.
     */
    private int compensationRetryAttempts = 2;

    /** Base URLs of the three business microservices. */
    private Services services = new Services();

    /** Read timeout / connect timeout for the inter-service HTTP calls, in milliseconds. */
    private int connectTimeoutMs = 3_000;
    private int readTimeoutMs = 15_000;

    @Data
    public static class Services {
        private String orderUrl = "http://localhost:8081";
        private String inventoryUrl = "http://localhost:8082";
        private String paymentUrl = "http://localhost:8083";
    }
}
