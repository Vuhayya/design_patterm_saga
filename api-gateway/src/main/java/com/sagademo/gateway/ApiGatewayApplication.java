package com.sagademo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API GATEWAY (Spring Cloud Gateway) - the single entry point on port 8080.
 *
 * <pre>
 *   React frontend (:5173)
 *          |
 *          v
 *   API Gateway (:8080)
 *          |
 *          +--> /api/orders/**     -> order-service      (:8081)
 *          +--> /api/inventory/**  -> inventory-service   (:8082)
 *          +--> /api/payments/**   -> payment-service     (:8083)
 *          +--> /api/sagas/**      -> saga-orchestrator   (:8084)
 * </pre>
 *
 * <p>Why a gateway belongs in this demo: the browser talks to ONE origin, which means one CORS
 * configuration and one base URL in the frontend, while the backend is still four independent
 * processes. It also makes the point visible - the frontend has no idea how many services exist
 * behind :8080.
 *
 * <p>The routes are declared in {@code application.yml} so they can be read as documentation.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
