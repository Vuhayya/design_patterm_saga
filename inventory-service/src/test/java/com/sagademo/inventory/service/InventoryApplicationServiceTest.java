package com.sagademo.inventory.service;

import com.sagademo.inventory.dto.ReleaseInventoryRequest;
import com.sagademo.inventory.dto.ReserveInventoryRequest;
import com.sagademo.inventory.entity.InventoryReservation;
import com.sagademo.inventory.entity.ReservationStatus;
import com.sagademo.inventory.exception.InsufficientStockException;
import com.sagademo.inventory.exception.SimulatedFailureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Local-transaction tests for the Inventory Service (T2 and its compensation C2).
 *
 * <p>The most important assertion in this class is
 * {@link #releaseRestoresTheExactStockLevel()} - it is the automated proof of what the
 * STATE COMPARISON panel shows on screen: after compensation the stock is back to 100.
 */
@SpringBootTest
class InventoryApplicationServiceTest {

    private static final String PRODUCT = "PIZZA-01";

    @Autowired
    private InventoryApplicationService inventoryService;

    @Autowired
    private FailureSimulationService failureSimulation;

    @BeforeEach
    void reset() {
        inventoryService.resetAllData();
        failureSimulation.reset();
    }

    private ReserveInventoryRequest reserveRequest(String sagaId, int qty, boolean fail) {
        return ReserveInventoryRequest.builder()
                .sagaId(sagaId)
                .orderId("ORDER-0001")
                .productId(PRODUCT)
                .quantity(qty)
                .simulateFailure(fail)
                .build();
    }

    @Test
    @DisplayName("T2 RESERVE_INVENTORY holds units and lowers available stock")
    void reserveHoldsUnits() {
        int before = inventoryService.findItem(PRODUCT).getAvailableUnits();

        InventoryReservation reservation = inventoryService.reserveInventory(reserveRequest("SAGA-1", 1, false));

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(inventoryService.findItem(PRODUCT).getAvailableUnits()).isEqualTo(before - 1);
        assertThat(inventoryService.findItem(PRODUCT).getTotalUnits()).isEqualTo(100);
    }

    @Test
    @DisplayName("T2 really fails and holds nothing when failure is simulated (scenario 3)")
    void reserveFailsWithoutSideEffects() {
        assertThatThrownBy(() -> inventoryService.reserveInventory(reserveRequest("SAGA-2", 1, true)))
                .isInstanceOf(SimulatedFailureException.class);

        assertThat(inventoryService.findItem(PRODUCT).getAvailableUnits()).isEqualTo(100);
        assertThat(inventoryService.findAllReservations()).isEmpty();
    }

    @Test
    @DisplayName("C2 RELEASE_INVENTORY restores the exact stock level that existed before the saga")
    void releaseRestoresTheExactStockLevel() {
        int before = inventoryService.findItem(PRODUCT).getAvailableUnits();
        InventoryReservation reservation = inventoryService.reserveInventory(reserveRequest("SAGA-3", 3, false));
        assertThat(inventoryService.findItem(PRODUCT).getAvailableUnits()).isEqualTo(before - 3);

        InventoryReservation released = inventoryService.releaseInventory(ReleaseInventoryRequest.builder()
                .sagaId("SAGA-3")
                .reservationId(reservation.getReservationId())
                .reason("payment failed")
                .build());

        assertThat(released.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(inventoryService.findItem(PRODUCT).getAvailableUnits()).isEqualTo(before);
    }

    @Test
    @DisplayName("C2 can compensate using only the sagaId (no reservationId needed)")
    void releaseBySagaIdOnly() {
        inventoryService.reserveInventory(reserveRequest("SAGA-4", 2, false));

        InventoryReservation released = inventoryService.releaseInventory(
                ReleaseInventoryRequest.builder().sagaId("SAGA-4").build());

        assertThat(released.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(inventoryService.findItem(PRODUCT).getAvailableUnits()).isEqualTo(100);
    }

    @Test
    @DisplayName("C2 is idempotent - a retried release must not credit the stock twice")
    void releaseIsIdempotent() {
        inventoryService.reserveInventory(reserveRequest("SAGA-5", 5, false));
        ReleaseInventoryRequest release = ReleaseInventoryRequest.builder().sagaId("SAGA-5").build();

        inventoryService.releaseInventory(release);
        inventoryService.releaseInventory(release);

        assertThat(inventoryService.findItem(PRODUCT).getAvailableUnits()).isEqualTo(100);
    }

    @Test
    @DisplayName("C2 itself can fail (scenario 5) and then the stock stays held")
    void releaseCanFail() {
        inventoryService.reserveInventory(reserveRequest("SAGA-6", 1, false));

        assertThatThrownBy(() -> inventoryService.releaseInventory(ReleaseInventoryRequest.builder()
                .sagaId("SAGA-6").simulateFailure(true).build()))
                .isInstanceOf(SimulatedFailureException.class);

        // The units are STILL held: this is the inconsistency a failed compensation leaves behind.
        assertThat(inventoryService.findItem(PRODUCT).getAvailableUnits()).isEqualTo(99);
    }

    @Test
    @DisplayName("A genuine business rejection (not enough stock) is reported as a conflict")
    void insufficientStockIsRejected() {
        assertThatThrownBy(() -> inventoryService.reserveInventory(reserveRequest("SAGA-7", 5_000, false)))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("T2 is idempotent per sagaId - a retry must not reserve twice")
    void reserveIsIdempotentPerSaga() {
        inventoryService.reserveInventory(reserveRequest("SAGA-8", 4, false));
        inventoryService.reserveInventory(reserveRequest("SAGA-8", 4, false));

        assertThat(inventoryService.findItem(PRODUCT).getAvailableUnits()).isEqualTo(96);
        assertThat(inventoryService.findAllReservations()).hasSize(1);
    }
}
