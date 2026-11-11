package com.sagademo.payment.service.impl;

import com.sagademo.payment.dto.SimulationSettingsDto;
import com.sagademo.payment.service.FailureSimulationService;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Holds the sticky failure switches of the Payment Service (demo tooling, never persisted). */
@Slf4j
@Service
public class FailureSimulationServiceImpl implements FailureSimulationService {

    private final AtomicBoolean failProcessPayment = new AtomicBoolean(false);
    private final AtomicBoolean failRefundPayment = new AtomicBoolean(false);

    @Override
    public SimulationSettingsDto current() {
        return SimulationSettingsDto.builder()
                .failProcessPayment(failProcessPayment.get())
                .failRefundPayment(failRefundPayment.get())
                .build();
    }

    @Override
    public SimulationSettingsDto update(SimulationSettingsDto settings) {
        failProcessPayment.set(settings.isFailProcessPayment());
        failRefundPayment.set(settings.isFailRefundPayment());
        log.warn("FAILURE SIMULATION UPDATED | processPayment={} refundPayment={}",
                settings.isFailProcessPayment(), settings.isFailRefundPayment());
        return current();
    }

    @Override
    public SimulationSettingsDto reset() {
        return update(new SimulationSettingsDto());
    }

    @Override
    public boolean shouldFailProcess(boolean requestFlag) {
        return requestFlag || failProcessPayment.get();
    }

    @Override
    public boolean shouldFailRefund(boolean requestFlag) {
        return requestFlag || failRefundPayment.get();
    }
}
