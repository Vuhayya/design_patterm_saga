package com.sagademo.order.controller;

import com.sagademo.order.dto.SimulationSettingsDto;
import com.sagademo.order.service.FailureSimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Demo/presentation endpoints of the Order Service.
 *
 * <ul>
 *   <li>{@code GET  /api/orders/health}     - used by the dashboard "ONLINE" indicator</li>
 *   <li>{@code GET  /api/orders/simulation} - read the sticky failure switches</li>
 *   <li>{@code POST /api/orders/simulation} - turn sticky failure switches on/off</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderSimulationController {

    private final FailureSimulationService failureSimulation;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "service", "order-service",
                "status", "UP",
                "port", 8081,
                "database", "ORDER DB (H2, owned exclusively by order-service)",
                "simulation", failureSimulation.current(),
                "timestamp", Instant.now()));
    }

    @GetMapping("/simulation")
    public ResponseEntity<SimulationSettingsDto> getSimulation() {
        return ResponseEntity.ok(failureSimulation.current());
    }

    @PostMapping("/simulation")
    public ResponseEntity<SimulationSettingsDto> updateSimulation(@RequestBody SimulationSettingsDto settings) {
        return ResponseEntity.ok(failureSimulation.update(settings));
    }

    @DeleteMapping("/simulation")
    public ResponseEntity<SimulationSettingsDto> resetSimulation() {
        return ResponseEntity.ok(failureSimulation.reset());
    }
}
