package com.sagademo.payment.service;

import com.sagademo.payment.dto.SimulationSettingsDto;

/**
 * Holds the sticky failure switches of the Payment Service (demo tooling, never persisted).
 *
 * <p>Contract only. Controllers and other services depend on this interface, never on the
 * implementation class.
 *
 * @see com.sagademo.payment.service.impl.FailureSimulationServiceImpl
 */
public interface FailureSimulationService {

    SimulationSettingsDto current();

    SimulationSettingsDto update(SimulationSettingsDto settings);

    SimulationSettingsDto reset();

    boolean shouldFailProcess(boolean requestFlag);

    boolean shouldFailRefund(boolean requestFlag);
}
