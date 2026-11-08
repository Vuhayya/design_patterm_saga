package com.sagademo.saga.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sagademo.saga.service.ServiceCallRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Instant;

/**
 * THE SINGLE PLACE where the Saga Orchestrator performs network I/O.
 *
 * <p>Every call through this class:
 * <ol>
 *   <li>adds the tracing headers {@code X-Saga-Id}, {@code X-Order-Id}, {@code X-Saga-Step}
 *       (that is how one saga becomes greppable across five service logs),</li>
 *   <li>measures how long the call took,</li>
 *   <li>writes a {@link com.sagademo.saga.entity.ServiceCallLog} row - the data behind the
 *       "SERVICE COMMUNICATION" panel of the UI,</li>
 *   <li>translates a non-2xx response (or an unreachable service) into a
 *       {@link RemoteServiceException}, which is what makes the orchestrator compensate.</li>
 * </ol>
 *
 * <p>There is no hidden magic and no retry here: one call in, one call out. That keeps the
 * Saga mechanism itself easy to follow during the presentation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaHttpClient {

    private final RestClient restClient;
    private final ServiceCallRecorder serviceCallRecorder;
    private final ObjectMapper objectMapper;

    /**
     * Performs a POST to another microservice and returns the deserialised body.
     *
     * @throws RemoteServiceException if the service answered with an error status or was unreachable
     */
    public <T> T post(SagaCallContext context,
                      String targetService,
                      String url,
                      Object requestBody,
                      Class<T> responseType) {

        String requestSummary = toJson(requestBody);
        Instant startedAt = Instant.now();
        long start = System.nanoTime();

        log.info("--> POST {} | step={} | service={}", url, context.stepName(), targetService);

        ResponseEntity<String> response;
        try {
            response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Saga-Id", nullSafe(context.sagaId()))
                    .header("X-Order-Id", nullSafe(context.orderId()))
                    .header("X-Saga-Step", context.stepName() == null ? "" : context.stepName().name())
                    .body(requestBody == null ? "{}" : requestBody)
                    .retrieve()
                    .toEntity(String.class);
        } catch (ResourceAccessException ex) {
            // The service process is not listening at all (stopped / crashed / wrong port).
            long durationMs = millisSince(start);
            serviceCallRecorder.record(context, targetService, "POST", url, null, false,
                    requestSummary, "connection failed: " + ex.getMessage(), startedAt, durationMs);
            log.error("<-- POST {} | CONNECTION FAILED after {} ms | {}", url, durationMs, ex.getMessage());
            throw RemoteServiceException.unreachable(targetService, url, ex.getMostSpecificCause().getMessage());
        }

        long durationMs = millisSince(start);
        HttpStatusCode status = response.getStatusCode();
        String body = response.getBody();
        boolean success = status.is2xxSuccessful();

        serviceCallRecorder.record(context, targetService, "POST", url, status.value(), success,
                requestSummary, body, startedAt, durationMs);

        if (!success) {
            RemoteServiceException failure = toRemoteFailure(targetService, status.value(), body);
            log.error("<-- POST {} | HTTP {} in {} ms | {}", url, status.value(), durationMs,
                    failure.getMessage());
            throw failure;
        }

        log.info("<-- POST {} | HTTP {} in {} ms", url, status.value(), durationMs);
        return deserialize(body, responseType, targetService);
    }

    /**
     * Plain GET used for health checks and for reading the state snapshots.
     * These calls are NOT part of the business transaction, so they are not written to the
     * service-communication log - otherwise the panel would be flooded with polling noise.
     *
     * @return the deserialised body, or {@code null} if the service is unreachable or answered with an error
     */
    public <T> T getQuietly(String url, Class<T> responseType) {
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .toEntity(String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                return null;
            }
            return deserialize(response.getBody(), responseType, url);
        } catch (Exception ex) {
            log.debug("GET {} failed: {}", url, ex.getMessage());
            return null;
        }
    }

    /**
     * POST that is NOT part of a business transaction (toggling failure switches, resetting
     * databases for the next demo run). Not written to the service-communication log.
     */
    public <T> T postQuietly(String url, Object body, Class<T> responseType) {
        try {
            ResponseEntity<String> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body == null ? "{}" : body)
                    .retrieve()
                    .toEntity(String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                return null;
            }
            return deserialize(response.getBody(), responseType, url);
        } catch (Exception ex) {
            log.debug("POST {} failed: {}", url, ex.getMessage());
            return null;
        }
    }

    /** DELETE used only by the demo "reset everything" helper. */
    public boolean deleteQuietly(String url) {
        try {
            ResponseEntity<String> response = restClient.delete().uri(url).retrieve().toEntity(String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception ex) {
            log.debug("DELETE {} failed: {}", url, ex.getMessage());
            return false;
        }
    }

    /** Same as {@link #getQuietly} but reports how long the call took - used by the health panel. */
    public <T> ProbeResult<T> probe(String url, Class<T> responseType) {
        long start = System.nanoTime();
        try {
            ResponseEntity<String> response = restClient.get().uri(url).retrieve().toEntity(String.class);
            long durationMs = millisSince(start);
            if (!response.getStatusCode().is2xxSuccessful()) {
                return new ProbeResult<>(null, durationMs, "HTTP " + response.getStatusCode().value());
            }
            return new ProbeResult<>(deserialize(response.getBody(), responseType, url), durationMs, null);
        } catch (Exception ex) {
            return new ProbeResult<>(null, millisSince(start), ex.getMessage());
        }
    }

    /** Outcome of a health probe: the body (or null), how long it took, and the failure reason. */
    public record ProbeResult<T>(T body, long durationMs, String error) {
        public boolean isUp() {
            return body != null && error == null;
        }
    }

    // ------------------------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------------------------

    /**
     * Turns a service's error envelope into a {@link RemoteServiceException}.
     * Every service in this demo answers with {@code {success, service, errorCode, message, ...}},
     * which is why the orchestrator can always show a precise reason in the UI.
     */
    private RemoteServiceException toRemoteFailure(String targetService, int status, String body) {
        String errorCode = "REMOTE_ERROR";
        String message = body == null ? "no response body" : body;
        try {
            JsonNode json = objectMapper.readTree(body);
            if (json.hasNonNull("errorCode")) {
                errorCode = json.get("errorCode").asText();
            }
            if (json.hasNonNull("message")) {
                message = json.get("message").asText();
            }
        } catch (Exception ignored) {
            // Not JSON - keep the raw body as the message.
        }
        return new RemoteServiceException(targetService, status, errorCode, message);
    }

    private <T> T deserialize(String body, Class<T> responseType, String source) {
        if (responseType == Void.class || body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(body, responseType);
        } catch (Exception ex) {
            throw new RemoteServiceException(source, 200, "INVALID_RESPONSE",
                    "Could not read the response of " + source + ": " + ex.getMessage());
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    private long millisSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
