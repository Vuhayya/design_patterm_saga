package com.sagademo.inventory.service;

import com.sagademo.inventory.dto.SimulationSettingsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/** Holds the sticky failure switches of the Inventory Service (demo tooling, never persisted). */
@Slf4j
@Service
public class FailureSimulationService {

    private final AtomicBoolean failReserveInventory = new AtomicBoolean(false);
    private final AtomicBoolean failReleaseInventory = new AtomicBoolean(false);

    public SimulationSettingsDto current() {
        return SimulationSettingsDto.builder()
                .failReserveInventory(failReserveInventory.get())
                .failReleaseInventory(failReleaseInventory.get())
                .build();
    }

    public SimulationSettingsDto update(SimulationSettingsDto settings) {
        failReserveInventory.set(settings.isFailReserveInventory());
        failReleaseInventory.set(settings.isFailReleaseInventory());
        log.warn("FAILURE SIMULATION UPDATED | reserveInventory={} releaseInventory={}",
                settings.isFailReserveInventory(), settings.isFailReleaseInventory());
        return current();
    }

    public SimulationSettingsDto reset() {
        return update(new SimulationSettingsDto());
    }

    public boolean shouldFailReserve(boolean requestFlag) {
        return requestFlag || failReserveInventory.get();
    }

    public boolean shouldFailRelease(boolean requestFlag) {
        return requestFlag || failReleaseInventory.get();
    }
}
