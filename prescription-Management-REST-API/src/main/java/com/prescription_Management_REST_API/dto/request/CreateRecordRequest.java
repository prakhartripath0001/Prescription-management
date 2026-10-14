package com.prescription_Management_REST_API.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record CreateRecordRequest(
        @NotNull @Positive
        Long userId,

        @NotNull @Positive
        Long clientId,

        @NotNull @PastOrPresent
        LocalDate recordDate,

        @NotBlank @Size(max = 2000)
        String diagnosis,

        @NotEmpty @Size(max = 100)
        List<@Valid RecordEntryRequest> entries
) {}
