package com.prescription_Management_REST_API.exception;

import com.prescription_Management_REST_API.dto.response.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized, exhaustive error handler.
 *
 * Rules enforced here:
 *  - Every exception produces a structured {@link ErrorResponse} — never a stack trace.
 *  - Internal details (DB messages, exception class names) are NEVER sent to the client.
 *  - All unexpected errors are logged server-side at ERROR level for diagnostics.
 *
 * HTTP status codes used:
 *  400 Bad Request        – validation failures, bad input, missing/malformed body
 *  404 Not Found          – entity not found, unknown route
 *  405 Method Not Allowed – wrong HTTP verb for a valid URL
 *  409 Conflict           – duplicate idempotency key
 *  415 Unsupported Media  – request body is not application/json
 *  500 Internal Server    – any unhandled exception (generic safe message)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 400: @Valid field-level validation errors ──────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));

        return badRequest(ErrorResponse.validation(fieldErrors));
    }

    // ── 400: Malformed JSON / missing request body ─────────────────────────────
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex) {
        return badRequest(ErrorResponse.of(400, "Bad Request",
                "Request body is missing or contains invalid JSON. " +
                "Please send a valid JSON object."));
    }

    // ── 400: Wrong type for a path variable or request param (e.g. /records/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        String expected = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "unknown";
        return badRequest(ErrorResponse.of(400, "Bad Request",
                "Parameter '" + ex.getName() + "' must be of type " + expected + "."));
    }

    // ── 400: Required query parameter is absent ────────────────────────────────
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex) {
        return badRequest(ErrorResponse.of(400, "Bad Request",
                "Required query parameter '" + ex.getParameterName() + "' is missing."));
    }

    // ── 400: Required header is absent ────────────────────────────────────────
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(
            MissingRequestHeaderException ex) {
        return badRequest(ErrorResponse.of(400, "Bad Request",
                "Required header '" + ex.getHeaderName() + "' is missing."));
    }

    // ── 400: Business-rule violations (service-layer guards) ──────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex) {
        return badRequest(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    // ── 404: Entity not found (User / Client / Record) ────────────────────────
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    // ── 404: Request to an undefined route ────────────────────────────────────
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleRouteNotFound(
            NoResourceFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found",
                        "The requested endpoint does not exist."));
    }

    // ── 405: Correct URL but wrong HTTP method ─────────────────────────────────
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of(405, "Method Not Allowed",
                        "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint."));
    }

    // ── 409: Duplicate idempotency key — unique constraint on DB ──────────────
    // NOTE: We intentionally do NOT forward ex.getMessage() — it would expose table/column names.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateKey(
            DataIntegrityViolationException ex) {
        // Log internally so we can diagnose, but never send DB details to the client.
        log.warn("DataIntegrityViolationException caught — likely duplicate idempotency key.", ex);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict",
                        "A record with this idempotency key already exists. " +
                        "If this was a network retry, re-send the same key " +
                        "to receive the original response."));
    }

    // ── 415: Client sent a non-JSON content type ──────────────────────────────
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.of(415, "Unsupported Media Type",
                        "Content-Type must be 'application/json'."));
    }

    // ── 500: Catch-all — logs internally, never leaks details to client ────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        // Log the full exception server-side for debugging.
        log.error("Unhandled exception — returning 500 to client.", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error",
                        "An unexpected error occurred. Please try again later."));
    }

    // ─── helper ───────────────────────────────────────────────────────────────
    private static ResponseEntity<ErrorResponse> badRequest(ErrorResponse body) {
        return ResponseEntity.badRequest().body(body);
    }
}
