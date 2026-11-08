package com.sagademo.saga.client;

import com.sagademo.saga.client.dto.InventoryItemClientResponse;
import com.sagademo.saga.client.dto.ReleaseInventoryPayload;
import com.sagademo.saga.client.dto.ReservationClientResponse;
import com.sagademo.saga.client.dto.ReserveInventoryPayload;
import com.sagademo.saga.config.SagaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/** The orchestrator's gateway to INVENTORY-SERVICE (:8082). */
@Component
@RequiredArgsConstructor
public class InventoryClient {

    public static final String SERVICE_NAME = "inventory-service";

    private final SagaHttpClient http;
    private final SagaProperties properties;

    public String baseUrl() {
        return properties.getServices().getInventoryUrl();
    }

    /** Saga step 2: RESERVE_INVENTORY (local transaction T2 in inventory-service). */
    public ReservationClientResponse reserve(SagaCallContext context, ReserveInventoryPayload payload) {
        return http.post(context, SERVICE_NAME, baseUrl() + "/api/inventory/reserve", payload,
                ReservationClientResponse.class);
    }

    /** COMPENSATION C2: RELEASE_INVENTORY (compensating transaction in inventory-service). */
    public ReservationClientResponse release(SagaCallContext context, ReleaseInventoryPayload payload) {
        return http.post(context, SERVICE_NAME, baseUrl() + "/api/inventory/release", payload,
                ReservationClientResponse.class);
    }

    /** Reads stock for one product - used by the STATE COMPARISON panel. */
    public InventoryItemClientResponse getItem(String productId) {
        return http.getQuietly(baseUrl() + "/api/inventory/" + productId,
                InventoryItemClientResponse.class);
    }

    /** Reads the whole menu - used by the product dropdown of the order form. */
    public InventoryItemClientResponse[] getAllItems() {
        return http.getQuietly(baseUrl() + "/api/inventory", InventoryItemClientResponse[].class);
    }

    public SagaHttpClient.ProbeResult<Map> health() {
        return http.probe(baseUrl() + "/api/inventory/health", Map.class);
    }

    public Map updateSimulation(Map<String, Boolean> settings) {
        return http.postQuietly(baseUrl() + "/api/inventory/simulation", settings, Map.class);
    }

    public Map getSimulation() {
        return http.getQuietly(baseUrl() + "/api/inventory/simulation", Map.class);
    }

    /** Demo helper: full stock, no reservations. */
    public boolean reset() {
        http.deleteQuietly(baseUrl() + "/api/inventory/simulation");
        return http.deleteQuietly(baseUrl() + "/api/inventory/reset");
    }
}
