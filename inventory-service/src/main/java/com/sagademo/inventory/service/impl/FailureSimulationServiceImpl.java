package com.sagademo.inventory.service.impl;

import com.sagademo.inventory.dto.SimulationSettingsDto;
import com.sagademo.inventory.service.FailureSimulationService;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Holds the sticky failure switches of the Inventory Service (demo tooling, never persisted). */
@Slf4j
@Service
public class FailureSimulationServiceImpl implements FailureSimulationService {

    private final AtomicBoolean failReserveInventory = new AtomicBoolean(false);
    private final AtomicBoolean failReleaseInventory = new AtomicBoolean(false);

    @Override
    public SimulationSettingsDto current() {
        return SimulationSettingsDto.builder()
                .failReserveInventory(failReserveInventory.get())
                .failReleaseInventory(failReleaseInventory.get())
                .build();
    }

    @Override
    public SimulationSettingsDto update(SimulationSettingsDto settings) {
        failReserveInventory.set(settings.isFailReserveInventory());
        failReleaseInventory.set(settings.isFailReleaseInventory());
        log.warn("FAILURE SIMULATION UPDATED | reserveInventory={} releaseInventory={}",
                settings.isFailReserveInventory(), settings.isFailReleaseInventory());
        return current();
    }

    @Override
    public SimulationSettingsDto reset() {
        return update(new SimulationSettingsDto());
    }

    @Override
    public boolean shouldFailReserve(boolean requestFlag) {
        return requestFlag || failReserveInventory.get();
    }

    @Override
    public boolean shouldFailRelease(boolean requestFlag) {
        return requestFlag || failReleaseInventory.get();
    }
}
