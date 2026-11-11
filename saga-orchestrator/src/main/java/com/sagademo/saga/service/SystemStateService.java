package com.sagademo.saga.service;

import com.sagademo.saga.dto.SystemStateDto;

/**
 * Builds the BEFORE / NOW / AFTER snapshots for the STATE COMPARISON panel (requirement #13).
 *
 * <p>Contract only. Controllers and other services depend on this interface, never on the
 * implementation class.
 *
 * @see com.sagademo.saga.service.impl.SystemStateServiceImpl
 */
public interface SystemStateService {

    /** Snapshot of the system as it is right now. */
    SystemStateDto capture(String label, String productId, String orderId, String paymentId);
}
