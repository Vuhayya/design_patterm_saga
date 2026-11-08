package com.sagademo.inventory.controller;

import com.sagademo.inventory.dto.InventoryItemResponse;
import com.sagademo.inventory.dto.ReleaseInventoryRequest;
import com.sagademo.inventory.dto.ReservationResponse;
import com.sagademo.inventory.dto.ReserveInventoryRequest;
import com.sagademo.inventory.entity.InventoryReservation;
import com.sagademo.inventory.service.InventoryApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Thin controller: HTTP in, DTO out, all logic in {@link InventoryApplicationService}. */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryApplicationService inventoryService;

    /** Saga step 2 (RESERVE_INVENTORY) - local transaction T2. */
    @PostMapping("/reserve")
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReserveInventoryRequest request) {
        InventoryReservation reservation = inventoryService.reserveInventory(request);
        return ResponseEntity.ok(ReservationResponse.of(
                reservation, inventoryService.findItem(reservation.getProductId())));
    }

    /** COMPENSATION for step 2 (RELEASE_INVENTORY) - compensating transaction C2. */
    @PostMapping("/release")
    public ResponseEntity<ReservationResponse> release(@Valid @RequestBody ReleaseInventoryRequest request) {
        InventoryReservation reservation = inventoryService.releaseInventory(request);
        return ResponseEntity.ok(ReservationResponse.of(
                reservation, inventoryService.findItem(reservation.getProductId())));
    }

    /** Stock snapshot of the whole menu - used by the STATE COMPARISON panel. */
    @GetMapping
    public ResponseEntity<List<InventoryItemResponse>> listItems() {
        return ResponseEntity.ok(
                inventoryService.findAllItems().stream().map(InventoryItemResponse::from).toList());
    }

    @GetMapping("/reservations")
    public ResponseEntity<List<ReservationResponse>> listReservations() {
        return ResponseEntity.ok(inventoryService.findAllReservations().stream()
                .map(reservation -> ReservationResponse.of(
                        reservation, inventoryService.findItem(reservation.getProductId())))
                .toList());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryItemResponse> getItem(@PathVariable String productId) {
        return ResponseEntity.ok(InventoryItemResponse.from(inventoryService.findItem(productId)));
    }

    /** Demo helper: full stock everywhere, no reservations. */
    @DeleteMapping("/reset")
    public ResponseEntity<Void> reset() {
        inventoryService.resetAllData();
        return ResponseEntity.noContent().build();
    }
}
