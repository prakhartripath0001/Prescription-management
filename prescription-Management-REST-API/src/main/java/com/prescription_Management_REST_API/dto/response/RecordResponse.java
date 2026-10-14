package com.prescription_Management_REST_API.dto.response;


import com.prescription_Management_REST_API.entity.enums.RecordStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RecordResponse(
        Long id,
        Long userId,
        Long clientId,
        LocalDate recordDate,
        String diagnosis,
        RecordStatus status,
        List<EntryResponse> entries
) {
    public record EntryResponse(
            Long id,
            String entryName,
            BigDecimal quantity,
            String frequency,
            Integer duration,
            String instructions
    ) {}
}
