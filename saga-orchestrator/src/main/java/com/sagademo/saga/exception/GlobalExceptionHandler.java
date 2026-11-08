package com.sagademo.saga.exception;

import com.sagademo.saga.client.RemoteServiceException;
import com.sagademo.saga.dto.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/** Maps exceptions to the uniform {@link ApiErrorResponse}. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SagaNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(SagaNotFoundException ex) {
        log.warn("SAGA NOT FOUND | {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("SAGA_NOT_FOUND", ex.getMessage(), null));
    }

    /**
     * Only reachable for calls made OUTSIDE a saga (health probes, simulation fan-out).
     * A {@code RemoteServiceException} raised during a saga step is caught by the orchestrator
     * itself and turned into compensation - it never reaches this handler.
     */
    @ExceptionHandler(RemoteServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleRemote(RemoteServiceException ex) {
        log.error("REMOTE SERVICE ERROR | {}", ex.describe());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiErrorResponse.of(ex.getErrorCode(), ex.describe(), null));
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
