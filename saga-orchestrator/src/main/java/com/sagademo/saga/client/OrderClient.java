package com.sagademo.saga.client;

import com.sagademo.saga.client.dto.CreateOrderPayload;
import com.sagademo.saga.client.dto.OrderActionPayload;
import com.sagademo.saga.client.dto.OrderClientResponse;
import com.sagademo.saga.config.SagaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The orchestrator's gateway to ORDER-SERVICE (:8081).
 *
 * <p>One method per remote operation, nothing else. Keeping the HTTP details in a client class
 * is what lets {@code SagaOrchestratorService} read like the state machine it is.
 */
@Component
@RequiredArgsConstructor
public class OrderClient {

    public static final String SERVICE_NAME = "order-service";

    private final SagaHttpClient http;
    private final SagaProperties properties;

    public String baseUrl() {
        return properties.getServices().getOrderUrl();
    }

    /** Saga step 1: CREATE_ORDER (local transaction T1 in order-service). */
    public OrderClientResponse createOrder(SagaCallContext context, CreateOrderPayload payload) {
        return http.post(context, SERVICE_NAME, baseUrl() + "/api/orders", payload,
                OrderClientResponse.class);
    }

    /** Saga step 4: CONFIRM_ORDER (local transaction T4 in order-service). */
    public OrderClientResponse confirmOrder(SagaCallContext context, String orderId, OrderActionPayload payload) {
        return http.post(context, SERVICE_NAME,
                baseUrl() + "/api/orders/" + orderId + "/confirm", payload, OrderClientResponse.class);
    }

    /** COMPENSATION C1: CANCEL_ORDER (compensating transaction in order-service). */
    public OrderClientResponse cancelOrder(SagaCallContext context, String orderId, OrderActionPayload payload) {
        return http.post(context, SERVICE_NAME,
                baseUrl() + "/api/orders/" + orderId + "/cancel", payload, OrderClientResponse.class);
    }

    /** Reads the current state of an order - used by the STATE COMPARISON panel. */
    public OrderClientResponse getOrder(String orderId) {
        return http.getQuietly(baseUrl() + "/api/orders/" + orderId, OrderClientResponse.class);
    }

    /** Health probe for the dashboard "ONLINE / OFFLINE" indicator. */
    public SagaHttpClient.ProbeResult<Map> health() {
        return http.probe(baseUrl() + "/api/orders/health", Map.class);
    }

    /** Pushes the sticky failure switches into order-service. */
    public Map updateSimulation(Map<String, Boolean> settings) {
        return http.postQuietly(baseUrl() + "/api/orders/simulation", settings, Map.class);
    }

    public Map getSimulation() {
        return http.getQuietly(baseUrl() + "/api/orders/simulation", Map.class);
    }

    /** Demo helper: wipe the Order database. */
    public boolean reset() {
        http.deleteQuietly(baseUrl() + "/api/orders/simulation");
        return http.deleteQuietly(baseUrl() + "/api/orders/reset");
    }
}
