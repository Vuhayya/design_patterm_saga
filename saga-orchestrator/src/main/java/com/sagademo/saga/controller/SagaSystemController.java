package com.sagademo.saga.controller;

import com.sagademo.saga.client.dto.InventoryItemClientResponse;
import com.sagademo.saga.dto.FailureSimulationDto;
import com.sagademo.saga.dto.ServiceHealthDto;
import com.sagademo.saga.entity.CompensationAction;
import com.sagademo.saga.saga.SagaDefinition;
import com.sagademo.saga.service.DemoControlService;
import com.sagademo.saga.service.SystemHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Dashboard and presentation endpoints of the orchestrator.
 *
 * <pre>
 *   GET    /api/sagas/health              is the orchestrator itself up?
 *   GET    /api/sagas/system/health       status of all four services (dashboard panel)
 *   GET    /api/sagas/definition          the step list and the compensation map, read from the code
 *   GET    /api/sagas/menu                the products, read from inventory-service
 *   GET    /api/sagas/simulation          current sticky failure switches
 *   POST   /api/sagas/simulation          set sticky failure switches on all services at once
 *   DELETE /api/sagas/reset               clear every database for a fresh demo run
 * </pre>
 */
@RestController
@RequestMapping("/api/sagas")
@RequiredArgsConstructor
public class SagaSystemController {

    private final SystemHealthService systemHealthService;
    private final DemoControlService demoControlService;
    private final SagaDefinition sagaDefinition;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "service", "saga-orchestrator",
                "status", "UP",
                "port", 8084,
                "database", "SAGA DB (H2, owned exclusively by saga-orchestrator)",
                "sagaStyle", "ORCHESTRATION",
                "timestamp", Instant.now()));
    }

    @GetMapping("/system/health")
    public ResponseEntity<List<ServiceHealthDto>> systemHealth() {
        return ResponseEntity.ok(systemHealthService.checkAll());
    }

    /**
     * THE SAGA DEFINITION, served straight out of the running code.
     *
     * <p>The "How Saga Works" page in the frontend renders this, so the diagram on screen can
     * never drift away from the implementation - it IS the implementation.
     */
    @GetMapping("/definition")
    public ResponseEntity<Map<String, Object>> definition() {
        List<Map<String, Object>> steps = sagaDefinition.steps().stream()
                .map(step -> Map.<String, Object>of(
                        "step", step.name().name(),
                        "targetService", step.targetService(),
                        "compensation", step.compensationName() == null
                                ? "NONE" : step.compensationName().name(),
                        "compensationService", step.compensationAction().targetService() == null
                                ? "-" : step.compensationAction().targetService(),
                        "hasCompensation", step.hasCompensation()))
                .toList();

        List<Map<String, Object>> compensationMap = Arrays.stream(CompensationAction.values())
                .map(action -> Map.<String, Object>of(
                        "forwardStep", action.forwardStep().name(),
                        "compensationStep", action.compensationStep() == null
                                ? "NONE" : action.compensationStep().name(),
                        "targetService", action.targetService() == null ? "-" : action.targetService()))
                .toList();

        return ResponseEntity.ok(Map.of(
                "sagaStyle", "ORCHESTRATION",
                "forwardSteps", steps,
                "compensationMap", compensationMap,
                "sagaStates", List.of("PENDING", "RUNNING", "COMPLETED", "FAILED",
                        "COMPENSATING", "COMPENSATED", "COMPENSATION_FAILED"),
                "stepStates", List.of("PENDING", "RUNNING", "COMPLETED", "FAILED",
                        "COMPENSATED", "COMPENSATION_FAILED", "SKIPPED")));
    }

    @GetMapping("/menu")
    public ResponseEntity<List<InventoryItemClientResponse>> menu() {
        return ResponseEntity.ok(demoControlService.menu());
    }

    @GetMapping("/simulation")
    public ResponseEntity<FailureSimulationDto> getSimulation() {
        return ResponseEntity.ok(demoControlService.readStickySimulation());
    }

    @PostMapping("/simulation")
    public ResponseEntity<FailureSimulationDto> setSimulation(@RequestBody FailureSimulationDto settings) {
        return ResponseEntity.ok(demoControlService.applyStickySimulation(settings));
    }

    @DeleteMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset() {
        return ResponseEntity.ok(demoControlService.resetEverything());
    }
}
