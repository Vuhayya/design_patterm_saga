package com.sagademo.order.service;

import com.sagademo.order.dto.SimulationSettingsDto;

/**
 * Holds the sticky, service-level failure switches (see {@code SimulationSettingsDto}).
 *
 * <p>Contract only. Controllers and other services depend on this interface, never on the
 * implementation class.
 *
 * @see com.sagademo.order.service.impl.FailureSimulationServiceImpl
 */
public interface FailureSimulationService {

    SimulationSettingsDto current();

    SimulationSettingsDto update(SimulationSettingsDto settings);

    SimulationSettingsDto reset();

    /** Effective decision for step CREATE_ORDER: per-request flag OR sticky flag. */
    boolean shouldFailCreateOrder(boolean requestFlag);

    /** Effective decision for step CONFIRM_ORDER. */
    boolean shouldFailConfirmOrder(boolean requestFlag);

    /** Effective decision for the COMPENSATION step CANCEL_ORDER. */
    boolean shouldFailCancelOrder(boolean requestFlag);
}
