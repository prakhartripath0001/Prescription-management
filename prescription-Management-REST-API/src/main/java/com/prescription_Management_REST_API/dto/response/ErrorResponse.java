package com.prescription_Management_REST_API.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response returned for all 4xx / 5xx cases.
 * fieldErrors is populated only for @Valid validation failures (400).
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp,
        Map<String, String> fieldErrors   // null unless this is a validation error
) {
    /** Convenience factory for simple errors (no field details). */
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, LocalDateTime.now(), null);
    }

    /** Convenience factory for Bean Validation errors with field-level detail. */
    public static ErrorResponse validation(Map<String, String> fieldErrors) {
        return new ErrorResponse(
                400, "Validation Failed",
                "One or more fields failed validation — see fieldErrors for details.",
                LocalDateTime.now(),
                fieldErrors
        );
    }
}
