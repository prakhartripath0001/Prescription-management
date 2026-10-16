package com.prescription_Management_REST_API.exception;

import com.prescription_Management_REST_API.dto.response.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized error handler — converts exceptions into structured {@link ErrorResponse} JSON.
 *
 * Handled cases:
 *  400 – Bean Validation failure (@Valid), malformed JSON body, illegal argument
 *  404 – EntityNotFoundException (User / Client / Record not found)
 *  409 – Duplicate idempotency key (DataIntegrityViolationException on unique column)
 *  500 – Unexpected server error
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 400: @Valid / Bean Validation field errors ────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(), fe.getDefaultMessage()));

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.validation(fieldErrors));
    }

    // ── 400: Malformed / unreadable JSON body ─────────────────────────────────
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex) {
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(400, "Bad Request",
                        "Request body is missing or contains invalid JSON."));
    }

    // ── 400: Business-rule violations (service-layer checks) ─────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex) {
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    // ── 400: Missing required header (e.g. X-Idempotency-Key when required) ──
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(
            MissingRequestHeaderException ex) {
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(400, "Bad Request",
                        "Required header '" + ex.getHeaderName() + "' is missing."));
    }

    // ── 404: Entity not found ─────────────────────────────────────────────────
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            EntityNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    // ── 409: Duplicate idempotency key (unique-constraint violation) ──────────
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
            DataIntegrityViolationException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict",
                        "A record with this idempotency key already exists. " +
                        "If this was a retry, fetch the original record instead."));
    }

    // ── 500: Catch-all for unexpected errors ──────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error",
                        "An unexpected error occurred. Please try again later."));
    }
}
