package com.sagademo.order.service;

import com.sagademo.order.dto.SimulationSettingsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Holds the sticky, service-level failure switches (see {@link SimulationSettingsDto}).
 *
 * <p>Kept in memory on purpose: it is demo tooling, not business state, so it must never
 * live in the Order database.
 */
@Slf4j
@Service
public class FailureSimulationService {

    private final AtomicBoolean failCreateOrder = new AtomicBoolean(false);
    private final AtomicBoolean failConfirmOrder = new AtomicBoolean(false);
    private final AtomicBoolean failCancelOrder = new AtomicBoolean(false);

    public SimulationSettingsDto current() {
        return SimulationSettingsDto.builder()
                .failCreateOrder(failCreateOrder.get())
                .failConfirmOrder(failConfirmOrder.get())
                .failCancelOrder(failCancelOrder.get())
                .build();
    }

    public SimulationSettingsDto update(SimulationSettingsDto settings) {
        failCreateOrder.set(settings.isFailCreateOrder());
        failConfirmOrder.set(settings.isFailConfirmOrder());
        failCancelOrder.set(settings.isFailCancelOrder());
        log.warn("FAILURE SIMULATION UPDATED | createOrder={} confirmOrder={} cancelOrder={}",
                settings.isFailCreateOrder(), settings.isFailConfirmOrder(), settings.isFailCancelOrder());
        return current();
    }

    public SimulationSettingsDto reset() {
        return update(new SimulationSettingsDto());
    }

    /** Effective decision for step CREATE_ORDER: per-request flag OR sticky flag. */
    public boolean shouldFailCreateOrder(boolean requestFlag) {
        return requestFlag || failCreateOrder.get();
    }

    /** Effective decision for step CONFIRM_ORDER. */
    public boolean shouldFailConfirmOrder(boolean requestFlag) {
        return requestFlag || failConfirmOrder.get();
    }

    /** Effective decision for the COMPENSATION step CANCEL_ORDER. */
    public boolean shouldFailCancelOrder(boolean requestFlag) {
        return requestFlag || failCancelOrder.get();
    }
}
