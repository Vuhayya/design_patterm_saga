package com.sagademo.saga.client;

import com.sagademo.saga.client.dto.PaymentClientResponse;
import com.sagademo.saga.client.dto.ProcessPaymentPayload;
import com.sagademo.saga.client.dto.RefundPaymentPayload;
import com.sagademo.saga.config.SagaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/** The orchestrator's gateway to PAYMENT-SERVICE (:8083). */
@Component
@RequiredArgsConstructor
public class PaymentClient {

    public static final String SERVICE_NAME = "payment-service";

    private final SagaHttpClient http;
    private final SagaProperties properties;

    public String baseUrl() {
        return properties.getServices().getPaymentUrl();
    }

    /** Saga step 3: PROCESS_PAYMENT (local transaction T3 in payment-service). */
    public PaymentClientResponse process(SagaCallContext context, ProcessPaymentPayload payload) {
        return http.post(context, SERVICE_NAME, baseUrl() + "/api/payments/process", payload,
                PaymentClientResponse.class);
    }

    /** COMPENSATION C3: REFUND_PAYMENT (compensating transaction in payment-service). */
    public PaymentClientResponse refund(SagaCallContext context, RefundPaymentPayload payload) {
        return http.post(context, SERVICE_NAME, baseUrl() + "/api/payments/refund", payload,
                PaymentClientResponse.class);
    }

    /** Reads a payment - used by the STATE COMPARISON panel. */
    public PaymentClientResponse getPayment(String paymentId) {
        return http.getQuietly(baseUrl() + "/api/payments/" + paymentId, PaymentClientResponse.class);
    }

    public SagaHttpClient.ProbeResult<Map> health() {
        return http.probe(baseUrl() + "/api/payments/health", Map.class);
    }

    public Map updateSimulation(Map<String, Boolean> settings) {
        return http.postQuietly(baseUrl() + "/api/payments/simulation", settings, Map.class);
    }

    public Map getSimulation() {
        return http.getQuietly(baseUrl() + "/api/payments/simulation", Map.class);
    }

    /** Demo helper: clear the payment ledger. */
    public boolean reset() {
        http.deleteQuietly(baseUrl() + "/api/payments/simulation");
        return http.deleteQuietly(baseUrl() + "/api/payments/reset");
    }
}
