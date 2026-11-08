package com.sagademo.inventory.service;

import com.sagademo.inventory.dto.ReleaseInventoryRequest;
import com.sagademo.inventory.dto.ReserveInventoryRequest;
import com.sagademo.inventory.entity.InventoryItem;
import com.sagademo.inventory.entity.InventoryReservation;
import com.sagademo.inventory.entity.ReservationStatus;
import com.sagademo.inventory.exception.InsufficientStockException;
import com.sagademo.inventory.exception.ResourceNotFoundException;
import com.sagademo.inventory.exception.SimulatedFailureException;
import com.sagademo.inventory.repository.InventoryItemRepository;
import com.sagademo.inventory.repository.InventoryReservationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ALL Inventory business logic. Each public method is ONE LOCAL TRANSACTION on the
 * Inventory database only.
 *
 * <pre>
 *   reserveInventory()  = T2  (local transaction, COMMITTED immediately)
 *   releaseInventory()  = C2  (COMPENSATING transaction for T2)
 * </pre>
 *
 * <p>Key teaching point: after {@code reserveInventory()} returns, the change is committed and
 * visible to everybody. If a later Saga step fails, there is no transaction left to roll back -
 * the only way out is to run {@code releaseInventory()}, a brand new committed transaction that
 * puts the units back.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryApplicationService {

    private static final String SERVICE_NAME = "INVENTORY_SERVICE";

    private final InventoryItemRepository itemRepository;
    private final InventoryReservationRepository reservationRepository;
    private final FailureSimulationService failureSimulation;

    @Value("${demo.processing-delay-ms:150}")
    private long processingDelayMs;

    private final AtomicLong sequence = new AtomicLong(0);

    @PostConstruct
    void initSequence() {
        sequence.set(reservationRepository.findMaxSequenceNo());
    }

    // ------------------------------------------------------------------------------------
    // T2 - RESERVE_INVENTORY (forward)
    // ------------------------------------------------------------------------------------

    /**
     * Local transaction T2: hold {@code quantity} units for this saga.
     *
     * <p>Idempotent by sagaId: if this saga already holds a RESERVED reservation, the existing
     * one is returned instead of double-reserving. An orchestrator that retries after a network
     * timeout must not consume stock twice.
     */
    @Transactional
    public InventoryReservation reserveInventory(ReserveInventoryRequest request) {
        logStep(request.getSagaId(), request.getOrderId(), "RESERVE_INVENTORY", "STARTED");
        simulateWork();

        if (failureSimulation.shouldFailReserve(request.isSimulateFailure())) {
            logStep(request.getSagaId(), request.getOrderId(), "RESERVE_INVENTORY", "FAILED");
            throw new SimulatedFailureException(
                    "INVENTORY_RESERVATION_FAILED",
                    "Simulated failure while reserving %d unit(s) of %s in inventory-service"
                            .formatted(request.getQuantity(), request.getProductId()),
                    request.getSagaId());
        }

        // Idempotency guard for retries.
        var existing = reservationRepository.findBySagaId(request.getSagaId());
        if (existing.isPresent() && existing.get().getStatus() == ReservationStatus.RESERVED) {
            log.info("Reservation for saga {} already exists ({}), returning it (idempotent)",
                    request.getSagaId(), existing.get().getReservationId());
            return existing.get();
        }

        InventoryItem item = findItem(request.getProductId());

        if (item.getAvailableUnits() < request.getQuantity()) {
            logStep(request.getSagaId(), request.getOrderId(), "RESERVE_INVENTORY", "REJECTED");
            throw new InsufficientStockException(
                    request.getProductId(), request.getQuantity(), item.getAvailableUnits());
        }

        item.setReservedUnits(item.getReservedUnits() + request.getQuantity());
        item.setUpdatedAt(Instant.now());
        itemRepository.save(item);

        String reservationId = "RES-%04d".formatted(sequence.incrementAndGet());
        InventoryReservation reservation = InventoryReservation.builder()
                .reservationId(reservationId)
                .sagaId(request.getSagaId())
                .orderId(request.getOrderId())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .status(ReservationStatus.RESERVED)
                .reservedAt(Instant.now())
                .sequenceNo(sequence.get())
                .build();
        reservationRepository.save(reservation);

        log.info("Reserved {} unit(s) of {} | available {} -> {}",
                request.getQuantity(), item.getProductId(),
                item.getAvailableUnits() + request.getQuantity(), item.getAvailableUnits());
        logStep(request.getSagaId(), request.getOrderId(), "RESERVE_INVENTORY", "COMPLETED");
        return reservation;
    }

    // ------------------------------------------------------------------------------------
    // C2 - RELEASE_INVENTORY (compensation for T2)
    // ------------------------------------------------------------------------------------

    /**
     * COMPENSATING TRANSACTION for T2: give the held units back.
     *
     * <p>Idempotent: releasing an already RELEASED reservation is a successful no-op. Without
     * this property a retried compensation would credit stock twice and corrupt the inventory -
     * a mistake that is very easy to make and very good to mention in the presentation.
     */
    @Transactional
    public InventoryReservation releaseInventory(ReleaseInventoryRequest request) {
        logStep(request.getSagaId(), null, "RELEASE_INVENTORY (compensation)", "STARTED");
        simulateWork();

        if (failureSimulation.shouldFailRelease(request.isSimulateFailure())) {
            logStep(request.getSagaId(), null, "RELEASE_INVENTORY (compensation)", "FAILED");
            throw new SimulatedFailureException(
                    "INVENTORY_RELEASE_FAILED",
                    "Simulated failure while releasing the inventory reservation of saga "
                            + request.getSagaId() + " - the compensating transaction itself failed",
                    request.getSagaId());
        }

        InventoryReservation reservation = resolveReservation(request);

        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            log.info("Reservation {} is already RELEASED - nothing to do (idempotent compensation)",
                    reservation.getReservationId());
            return reservation;
        }

        InventoryItem item = findItem(reservation.getProductId());
        item.setReservedUnits(Math.max(0, item.getReservedUnits() - reservation.getQuantity()));
        item.setUpdatedAt(Instant.now());
        itemRepository.save(item);

        reservation.setStatus(ReservationStatus.RELEASED);
        reservation.setReleasedAt(Instant.now());
        reservationRepository.save(reservation);

        log.info("Released {} unit(s) of {} | available is now {}",
                reservation.getQuantity(), item.getProductId(), item.getAvailableUnits());
        logStep(request.getSagaId(), reservation.getOrderId(),
                "RELEASE_INVENTORY (compensation)", "COMPENSATED");
        return reservation;
    }

    // ------------------------------------------------------------------------------------
    // Queries / demo helpers
    // ------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public InventoryItem findItem(String productId) {
        return itemRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown product: " + productId));
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> findAllItems() {
        return itemRepository.findAllByOrderByProductIdAsc();
    }

    @Transactional(readOnly = true)
    public List<InventoryReservation> findAllReservations() {
        return reservationRepository.findAllByOrderBySequenceNoDesc();
    }

    /** Demo helper: drop all reservations and put every product back to full stock. */
    @Transactional
    public void resetAllData() {
        reservationRepository.deleteAll();
        itemRepository.findAll().forEach(item -> {
            item.setReservedUnits(0);
            item.setUpdatedAt(Instant.now());
            itemRepository.save(item);
        });
        sequence.set(0);
        log.warn("{} | INVENTORY DATABASE RESET (demo helper)", SERVICE_NAME);
    }

    private InventoryReservation resolveReservation(ReleaseInventoryRequest request) {
        if (request.getReservationId() != null && !request.getReservationId().isBlank()) {
            return reservationRepository.findById(request.getReservationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Unknown reservation: " + request.getReservationId()));
        }
        return reservationRepository.findBySagaId(request.getSagaId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No reservation found for saga: " + request.getSagaId()));
    }

    private void logStep(String sagaId, String orderId, String stepName, String status) {
        log.info("[{}] [{}] [{}] {} {}",
                sagaId == null ? "no-saga" : sagaId,
                orderId == null ? "no-order" : orderId,
                SERVICE_NAME, stepName, status);
    }

    private void simulateWork() {
        if (processingDelayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(processingDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
