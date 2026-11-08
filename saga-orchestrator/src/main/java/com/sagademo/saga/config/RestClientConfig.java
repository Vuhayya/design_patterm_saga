package com.sagademo.saga.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * The HTTP client the orchestrator uses to talk to the other microservices.
 *
 * <p>Two details that matter for the demo:
 * <ul>
 *   <li><b>Timeouts</b> - if you stop a service in its terminal, the orchestrator fails fast with
 *       a connection error instead of hanging, so the UI can show SERVICE_UNAVAILABLE and start
 *       compensating.</li>
 *   <li><b>No automatic exception on 4xx/5xx</b> - {@code SagaHttpClient} wants to inspect the
 *       status code itself so it can (a) write a {@code ServiceCallLog} row for the call and
 *       (b) translate the service's error envelope into a {@code RemoteServiceException}.</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(SagaProperties.class)
public class RestClientConfig {

    @Bean
    public RestClient restClient(SagaProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    // Intentionally empty: error responses are handled in SagaHttpClient.
                })
                .build();
    }
}
