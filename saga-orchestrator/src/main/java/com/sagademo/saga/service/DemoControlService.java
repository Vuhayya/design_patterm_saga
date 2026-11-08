package com.sagademo.saga.service;

import com.sagademo.saga.client.InventoryClient;
import com.sagademo.saga.client.OrderClient;
import com.sagademo.saga.client.PaymentClient;
import com.sagademo.saga.client.dto.InventoryItemClientResponse;
import com.sagademo.saga.dto.FailureSimulationDto;
import com.sagademo.saga.repository.SagaExecutionRepository;
import com.sagademo.saga.repository.SagaLogEntryRepository;
import com.sagademo.saga.repository.SagaStepExecutionRepository;
import com.sagademo.saga.repository.ServiceCallLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PRESENTATION TOOLING (not part of the Saga pattern itself).
 *
 * <ul>
 *   <li>fan out the sticky failure switches to all three services with one call, so the
 *       dashboard checkboxes can be flipped from a single place;</li>
 *   <li>read the menu for the order form;</li>
 *   <li>reset every database so the demo can be run again from a clean state.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemoControlService {

    private final OrderClient orderClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;

    private final SagaExecutionRepository sagaRepository;
    private final SagaStepExecutionRepository stepRepository;
    private final ServiceCallLogRepository serviceCallLogRepository;
    private final SagaLogEntryRepository logRepository;

    /** Pushes one aggregated switch object down to the three services that own the steps. */
    public FailureSimulationDto applyStickySimulation(FailureSimulationDto settings) {
        orderClient.updateSimulation(Map.of(
                "failCreateOrder", settings.isFailCreateOrder(),
                "failConfirmOrder", settings.isFailConfirmOrder(),
                "failCancelOrder", settings.isFailCancelOrder()));

        inventoryClient.updateSimulation(Map.of(
                "failReserveInventory", settings.isFailReserveInventory(),
                "failReleaseInventory", settings.isFailReleaseInventory()));

        paymentClient.updateSimulation(Map.of(
                "failProcessPayment", settings.isFailProcessPayment(),
                "failRefundPayment", settings.isFailRefundPayment()));

        log.warn("Sticky failure simulation pushed to all services: {}", settings);
        return readStickySimulation();
    }

    /** Reads back what each service currently reports, so the UI never shows a stale switch. */
    public FailureSimulationDto readStickySimulation() {
        Map<String, Object> merged = new HashMap<>();
        putAll(merged, orderClient.getSimulation());
        putAll(merged, inventoryClient.getSimulation());
        putAll(merged, paymentClient.getSimulation());

        return FailureSimulationDto.builder()
                .failCreateOrder(flag(merged, "failCreateOrder"))
                .failConfirmOrder(flag(merged, "failConfirmOrder"))
                .failCancelOrder(flag(merged, "failCancelOrder"))
                .failReserveInventory(flag(merged, "failReserveInventory"))
                .failReleaseInventory(flag(merged, "failReleaseInventory"))
                .failProcessPayment(flag(merged, "failProcessPayment"))
                .failRefundPayment(flag(merged, "failRefundPayment"))
                .build();
    }

    /** The menu, read from the service that owns it. */
    public List<InventoryItemClientResponse> menu() {
        InventoryItemClientResponse[] items = inventoryClient.getAllItems();
        return items == null ? List.of() : List.of(items);
    }

    /**
     * Clears the Saga database and asks every business service to clear its own.
     * Each service resets ITS OWN data - the orchestrator cannot touch their tables.
     */
    @Transactional
    public Map<String, Object> resetEverything() {
        logRepository.deleteAll();
        serviceCallLogRepository.deleteAll();
        stepRepository.deleteAll();
        sagaRepository.deleteAll();

        boolean orderOk = orderClient.reset();
        boolean inventoryOk = inventoryClient.reset();
        boolean paymentOk = paymentClient.reset();

        log.warn("DEMO RESET | saga db cleared | order={} inventory={} payment={}",
                orderOk, inventoryOk, paymentOk);

        return Map.of(
                "success", orderOk && inventoryOk && paymentOk,
                "sagaDatabase", "CLEARED",
                "orderService", orderOk ? "CLEARED" : "UNREACHABLE",
                "inventoryService", inventoryOk ? "CLEARED" : "UNREACHABLE",
                "paymentService", paymentOk ? "CLEARED" : "UNREACHABLE");
    }

    private void putAll(Map<String, Object> target, Map source) {
        if (source == null) {
            return;
        }
        source.forEach((key, value) -> target.put(String.valueOf(key), value));
    }

    private boolean flag(Map<String, Object> source, String key) {
        return Boolean.TRUE.equals(source.get(key));
    }
}
