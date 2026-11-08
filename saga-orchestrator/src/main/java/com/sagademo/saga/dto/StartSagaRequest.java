package com.sagademo.saga.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * What the "CREATE ORDER / START SAGA" form posts to
 * {@code POST /api/sagas/orders} (through the API Gateway on :8080).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartSagaRequest {

    @NotBlank(message = "customerName is required")
    private String customerName;

    @NotBlank(message = "productId is required")
    private String productId;

    private String productName;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "unitPrice is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "unitPrice must be greater than 0")
    private BigDecimal unitPrice;

    /** Which services should deliberately fail during this particular saga. */
    @Valid
    @Builder.Default
    private FailureSimulationDto failureSimulation = new FailureSimulationDto();
}
