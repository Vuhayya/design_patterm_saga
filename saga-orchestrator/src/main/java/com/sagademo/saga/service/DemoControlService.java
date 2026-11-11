package com.sagademo.saga.service;

import com.sagademo.saga.client.dto.InventoryItemClientResponse;
import com.sagademo.saga.dto.FailureSimulationDto;
import java.util.List;
import java.util.Map;

/**
 * PRESENTATION TOOLING (not part of the Saga pattern itself).
 *
 * <p>Contract only. Controllers and other services depend on this interface, never on the
 * implementation class.
 *
 * @see com.sagademo.saga.service.impl.DemoControlServiceImpl
 */
public interface DemoControlService {

    /** Pushes one aggregated switch object down to the three services that own the steps. */
    FailureSimulationDto applyStickySimulation(FailureSimulationDto settings);

    /** Reads back what each service currently reports, so the UI never shows a stale switch. */
    FailureSimulationDto readStickySimulation();

    /** The menu, read from the service that owns it. */
    List<InventoryItemClientResponse> menu();

    /** Clears the Saga database and asks every business service to clear its own. */
    Map<String, Object> resetEverything();
}
