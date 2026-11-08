package com.sagademo.payment.controller;

import com.sagademo.payment.dto.SimulationSettingsDto;
import com.sagademo.payment.service.FailureSimulationService;
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

/** Health + sticky failure switches of the Payment Service. */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentSimulationController {

    private final FailureSimulationService failureSimulation;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "service", "payment-service",
                "status", "UP",
                "port", 8083,
                "database", "PAYMENT DB (H2, owned exclusively by payment-service)",
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
