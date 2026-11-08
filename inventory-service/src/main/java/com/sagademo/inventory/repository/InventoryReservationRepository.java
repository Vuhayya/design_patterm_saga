package com.sagademo.inventory.repository;

import com.sagademo.inventory.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/** Data access for reservations. Private to the Inventory Service. */
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, String> {

    List<InventoryReservation> findAllByOrderBySequenceNoDesc();

    /**
     * Used by the compensating transaction: the orchestrator may send either the
     * reservationId or only the sagaId, and both must work.
     */
    Optional<InventoryReservation> findBySagaId(String sagaId);

    @Query("select coalesce(max(r.sequenceNo), 0) from InventoryReservation r")
    long findMaxSequenceNo();
}
