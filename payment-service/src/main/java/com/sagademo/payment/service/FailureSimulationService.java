package com.sagademo.payment.service;

import com.sagademo.payment.dto.SimulationSettingsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/** Holds the sticky failure switches of the Payment Service (demo tooling, never persisted). */
@Slf4j
@Service
public class FailureSimulationService {

    private final AtomicBoolean failProcessPayment = new AtomicBoolean(false);
    private final AtomicBoolean failRefundPayment = new AtomicBoolean(false);

    public SimulationSettingsDto current() {
        return SimulationSettingsDto.builder()
                .failProcessPayment(failProcessPayment.get())
                .failRefundPayment(failRefundPayment.get())
                .build();
    }

    public SimulationSettingsDto update(SimulationSettingsDto settings) {
        failProcessPayment.set(settings.isFailProcessPayment());
        failRefundPayment.set(settings.isFailRefundPayment());
        log.warn("FAILURE SIMULATION UPDATED | processPayment={} refundPayment={}",
                settings.isFailProcessPayment(), settings.isFailRefundPayment());
        return current();
    }

    public SimulationSettingsDto reset() {
        return update(new SimulationSettingsDto());
    }

    public boolean shouldFailProcess(boolean requestFlag) {
        return requestFlag || failProcessPayment.get();
    }

    public boolean shouldFailRefund(boolean requestFlag) {
        return requestFlag || failRefundPayment.get();
    }
}
