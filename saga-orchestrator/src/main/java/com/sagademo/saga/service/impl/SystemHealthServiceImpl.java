package com.sagademo.saga.service.impl;

import com.sagademo.saga.client.InventoryClient;
import com.sagademo.saga.client.OrderClient;
import com.sagademo.saga.client.PaymentClient;
import com.sagademo.saga.client.SagaHttpClient;
import com.sagademo.saga.dto.ServiceHealthDto;
import com.sagademo.saga.service.SystemHealthService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Feeds the dashboard "Services" panel by really probing each microservice:
 *
 * <pre>
 *   Order Service       * ONLINE    localhost:8081
 *   Inventory Service   * ONLINE    localhost:8082
 *   Payment Service     * OFFLINE   localhost:8083   <- stop it in its terminal and watch this flip
 *   Saga Orchestrator   * ONLINE    localhost:8084
 * </pre>
 *
 * <p>Stopping a service during the presentation is a good extra demo: the Saga then fails with
 * {@code SERVICE_UNAVAILABLE} instead of a simulated business error, and compensation runs
 * exactly the same way.
 */
@Service
@RequiredArgsConstructor
public class SystemHealthServiceImpl implements SystemHealthService {

    private final OrderClient orderClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;

    @Override
    public List<ServiceHealthDto> checkAll() {
        List<ServiceHealthDto> result = new ArrayList<>();
        result.add(toDto("order-service", "Order Service", orderClient.baseUrl(), orderClient.health()));
        result.add(toDto("inventory-service", "Inventory Service", inventoryClient.baseUrl(),
                inventoryClient.health()));
        result.add(toDto("payment-service", "Payment Service", paymentClient.baseUrl(),
                paymentClient.health()));

        // The orchestrator obviously does not need an HTTP call to know that it is up.
        result.add(new ServiceHealthDto("saga-orchestrator", "Saga Orchestrator",
                "http://localhost:8084", "ONLINE", 0L,
                "SAGA DB (H2, owned exclusively by saga-orchestrator)", null, null));
        return result;
    }

    private ServiceHealthDto toDto(String service, String displayName, String baseUrl,
                                   SagaHttpClient.ProbeResult<Map> probe) {
        if (!probe.isUp()) {
            return new ServiceHealthDto(service, displayName, baseUrl, "OFFLINE",
                    probe.durationMs(), null, null, probe.error());
        }
        Map body = probe.body();
        return new ServiceHealthDto(service, displayName, baseUrl, "ONLINE", probe.durationMs(),
                asString(body.get("database")), body.get("simulation"), null);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
