package com.sagademo.inventory.service;

import com.sagademo.inventory.dto.SimulationSettingsDto;

/**
 * Holds the sticky failure switches of the Inventory Service (demo tooling, never persisted).
 *
 * <p>Contract only. Controllers and other services depend on this interface, never on the
 * implementation class.
 *
 * @see com.sagademo.inventory.service.impl.FailureSimulationServiceImpl
 */
public interface FailureSimulationService {

    SimulationSettingsDto current();

    SimulationSettingsDto update(SimulationSettingsDto settings);

    SimulationSettingsDto reset();

    boolean shouldFailReserve(boolean requestFlag);

    boolean shouldFailRelease(boolean requestFlag);
}
