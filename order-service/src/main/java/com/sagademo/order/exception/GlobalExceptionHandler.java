package com.sagademo.order.exception;

import com.sagademo.order.dto.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Turns every exception into the uniform {@link ApiErrorResponse} envelope, so the
 * Saga Orchestrator always receives a machine-readable reason for the failure.
 *
 * <p>Status code contract used by the orchestrator:
 * <ul>
 *   <li>400 - bad request payload (validation)</li>
 *   <li>404 - order does not exist</li>
 *   <li>409 - illegal state transition (business rule)</li>
 *   <li>500 - simulated / unexpected failure  ==&gt; triggers COMPENSATION</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SimulatedFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleSimulatedFailure(SimulatedFailureException ex) {
        log.error("SIMULATED FAILURE | code={} | {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(ex.getErrorCode(), ex.getMessage(), ex.getSagaId()));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(OrderNotFoundException ex) {
        log.warn("ORDER NOT FOUND | {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("ORDER_NOT_FOUND", ex.getMessage(), null));
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidState(InvalidOrderStateException ex) {
        log.warn("INVALID ORDER STATE | {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("INVALID_ORDER_STATE", ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("VALIDATION FAILED | {}", message);
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("VALIDATION_ERROR", message, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        log.error("UNEXPECTED ERROR", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("INTERNAL_ERROR", String.valueOf(ex.getMessage()), null));
    }
}
