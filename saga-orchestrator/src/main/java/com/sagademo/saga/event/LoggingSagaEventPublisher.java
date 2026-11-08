package com.sagademo.saga.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Version 1 implementation: events go to the console, not to a broker.
 *
 * <p>The log line is formatted exactly like requirement #17 asks, so one saga can be traced
 * across every service window:
 *
 * <pre>
 *   EVENT [SAGA-20260926-0003] [ORDER-0007] [PAYMENT_SERVICE] PROCESS_PAYMENT FAILED
 * </pre>
 */
@Slf4j
@Component
public class LoggingSagaEventPublisher implements SagaEventPublisher {

    @Override
    public void publish(SagaEvent event) {
        log.info("EVENT {} [{}] [{}] {} {} | {}",
                event.eventType(),
                event.sagaId(),
                event.orderId() == null ? "no-order" : event.orderId(),
                event.step() == null ? "-" : event.step(),
                event.status(),
                event.message() == null ? "" : event.message());
    }
}
