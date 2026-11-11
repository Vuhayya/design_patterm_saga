package com.sagademo.inventory.service;

import com.sagademo.inventory.dto.ReleaseInventoryRequest;
import com.sagademo.inventory.dto.ReserveInventoryRequest;
import com.sagademo.inventory.entity.InventoryItem;
import com.sagademo.inventory.entity.InventoryReservation;
import java.util.List;

/**
 * ALL Inventory business logic.
 *
 * <p>Contract only. Controllers and other services depend on this interface, never on the
 * implementation class.
 *
 * @see com.sagademo.inventory.service.impl.InventoryApplicationServiceImpl
 */
public interface InventoryApplicationService {

    /** Local transaction T2: hold {@code quantity} units for this saga. */
    InventoryReservation reserveInventory(ReserveInventoryRequest request);

    /** COMPENSATING TRANSACTION for T2: give the held units back. */
    InventoryReservation releaseInventory(ReleaseInventoryRequest request);

    InventoryItem findItem(String productId);

    List<InventoryItem> findAllItems();

    List<InventoryReservation> findAllReservations();

    /** Demo helper: drop all reservations and put every product back to full stock. */
    void resetAllData();
}
