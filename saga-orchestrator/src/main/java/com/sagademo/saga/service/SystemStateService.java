package com.sagademo.saga.service;

import com.sagademo.saga.client.InventoryClient;
import com.sagademo.saga.client.OrderClient;
import com.sagademo.saga.client.PaymentClient;
import com.sagademo.saga.client.dto.InventoryItemClientResponse;
import com.sagademo.saga.client.dto.OrderClientResponse;
import com.sagademo.saga.client.dto.PaymentClientResponse;
import com.sagademo.saga.dto.SystemStateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Builds the BEFORE / NOW / AFTER snapshots for the STATE COMPARISON panel (requirement #13).
 *
 * <p>It reads the three services over HTTP - it does NOT read their databases. That restriction is
 * the point: the orchestrator has no access to anybody else's tables, so the only way to know the
 * state of the system is to ask each owner.
 *
 * <pre>
 *   inventory-service  GET /api/inventory/{productId}  -> total / reserved / available units
 *   payment-service    GET /api/payments/{paymentId}   -> COMPLETED | FAILED | REFUNDED
 *   order-service      GET /api/orders/{orderId}       -> CREATED | CONFIRMED | CANCELLED
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemStateService {

    private final OrderClient orderClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;

    /**
     * Snapshot of the system as it is right now.
     *
     * @param orderId   may be null (no order created yet) -> reported as NOT_CREATED
     * @param paymentId may be null (no payment attempted yet) -> reported as NOT_PROCESSED
     */
    public SystemStateDto capture(String label, String productId, String orderId, String paymentId) {
        InventoryItemClientResponse item = productId == null ? null : inventoryClient.getItem(productId);

        String orderStatus = SystemStateDto.NOT_CREATED;
        if (orderId != null) {
            OrderClientResponse order = orderClient.getOrder(orderId);
            orderStatus = order == null ? SystemStateDto.UNKNOWN : order.status();
        }

        String paymentStatus = SystemStateDto.NOT_PROCESSED;
        String paymentReference = null;
        if (paymentId != null) {
            PaymentClientResponse payment = paymentClient.getPayment(paymentId);
            if (payment == null) {
                paymentStatus = SystemStateDto.UNKNOWN;
            } else {
                paymentStatus = payment.status();
                paymentReference = payment.refundReference() != null
                        ? payment.refundReference()
                        : payment.transactionReference();
            }
        }

        return new SystemStateDto(
                label,
                productId,
                item == null ? null : item.productName(),
                item == null ? null : item.totalUnits(),
                item == null ? null : item.reservedUnits(),
                item == null ? null : item.availableUnits(),
                paymentStatus,
                paymentReference,
                orderStatus,
                Instant.now());
    }
}
