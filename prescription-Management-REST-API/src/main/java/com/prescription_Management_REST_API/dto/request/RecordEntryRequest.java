package com.prescription_Management_REST_API.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record RecordEntryRequest(
        @NotBlank @Size(max = 150)
        String entryName,

        @NotNull @DecimalMin(value = "0.0", inclusive = false, message = "quantity must be greater than 0")
        @DecimalMax(value = "100000.0", message = "quantity must not exceed 100000")
        BigDecimal quantity,

        @NotBlank @Size(max = 100)
        String frequency,

        @NotNull @Positive @Max(3650)
        Integer duration,

        @Size(max = 1000)
        String instructions
) {}
