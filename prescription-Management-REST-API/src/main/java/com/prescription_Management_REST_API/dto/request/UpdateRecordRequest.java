package com.prescription_Management_REST_API.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record UpdateRecordRequest(

        @NotNull @PastOrPresent
        LocalDate recordDate,

        @NotBlank @Size(max = 2000)
        String diagnosis,

        @NotEmpty
        List<@Valid RecordEntryRequest> entries
) {}

