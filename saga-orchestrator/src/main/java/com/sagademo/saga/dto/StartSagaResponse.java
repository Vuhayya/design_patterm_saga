package com.sagademo.saga.dto;

/**
 * Immediate answer to "start a saga".
 *
 * <pre>
 * {
 *   "sagaId": "SAGA-0001",
 *   "orderId": null,
 *   "status": "RUNNING",
 *   "pollUrl": "/api/sagas/SAGA-0001"
 * }
 * </pre>
 *
 * <p>The saga keeps running in the background; the frontend then polls {@code pollUrl} and
 * animates the steps as the backend really completes them. When the saga is started
 * synchronously ({@code ?async=false}) this response already carries the final status.
 */
public record StartSagaResponse(
        boolean success,
        String sagaId,
        String orderId,
        String status,
        String pollUrl,
        String message
) {
}
