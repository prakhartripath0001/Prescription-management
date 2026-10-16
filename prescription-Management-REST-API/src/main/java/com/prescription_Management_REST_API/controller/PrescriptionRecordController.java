package com.prescription_Management_REST_API.controller;

import com.prescription_Management_REST_API.dto.request.CreateRecordRequest;
import com.prescription_Management_REST_API.dto.request.UpdateRecordRequest;
import com.prescription_Management_REST_API.dto.response.RecordResponse;
import com.prescription_Management_REST_API.service.PrescriptionRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/records")
@Tag(name = "Prescription Records", description = "CRUD operations for prescription records")
@SecurityRequirement(name = "basicAuth")
public class PrescriptionRecordController {

    private final PrescriptionRecordService service;

    public PrescriptionRecordController(PrescriptionRecordService service) {
        this.service = service;
    }

    /**
     * POST /api/v1/records
     *
     * Idempotency: supply a unique UUID in the X-Idempotency-Key header.
     * If the same key is sent again (e.g. network retry), the original record
     * is returned instead of creating a duplicate. The header is optional —
     * omitting it disables idempotency protection for that request.
     *
     * Returns: 201 Created
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new prescription record",
            description = "Creates a prescription record together with all its medication entries. " +
                    "Requires ROLE_ADMIN or ROLE_USER. Supports idempotency via X-Idempotency-Key header.",
            tags = {"Prescription Records"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Record created successfully",
                    content = @Content(schema = @Schema(implementation = RecordResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient role"),
            @ApiResponse(responseCode = "409", description = "Conflict - duplicate idempotency key"),
            @ApiResponse(responseCode = "415", description = "Unsupported media type - Content-Type must be application/json")
    })
    public RecordResponse create(
            @Parameter(description = "Optional idempotency key (UUID) to prevent duplicate creation on retry",
                    example = "11111111-1111-1111-1111-111111111111")
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Parameter(description = "Record creation request body", required = true)
            @Valid @RequestBody CreateRecordRequest request) {
        return service.create(request, idempotencyKey);
    }

    // GET /api/v1/records/{id}
    @GetMapping("/{id}")
    @Operation(
            summary = "Get a prescription record by ID",
            description = "Returns a single prescription record with all its entries. " +
                    "Requires ROLE_ADMIN or ROLE_USER.",
            tags = {"Prescription Records"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Record found",
                    content = @Content(schema = @Schema(implementation = RecordResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Record not found")
    })
    public RecordResponse getById(
            @Parameter(description = "ID of the prescription record", example = "1", required = true)
            @PathVariable Long id) {
        return service.getById(id);
    }

    // GET /api/v1/records?page=0&size=20
    @GetMapping
    @Operation(
            summary = "Get all prescription records (paginated)",
            description = "Returns a paginated list of all prescription records. " +
                    "Requires ROLE_ADMIN or ROLE_USER.",
            tags = {"Prescription Records"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of records"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public Page<RecordResponse> getAll(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return service.getAll(page, size);
    }

    // GET /api/v1/records/client/{clientId}?page=0&size=20
    @GetMapping("/client/{clientId}")
    @Operation(
            summary = "Get prescription records by client ID",
            description = "Returns a paginated list of prescription records for a specific client. " +
                    "Requires ROLE_ADMIN or ROLE_USER.",
            tags = {"Prescription Records"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of records"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    public Page<RecordResponse> getByClientId(
            @Parameter(description = "ID of the client", example = "1", required = true)
            @PathVariable Long clientId,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return service.getByClientId(clientId, page, size);
    }

    // GET /api/v1/records/user/{userId}?page=0&size=20
    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get prescription records by user (prescriber) ID",
            description = "Returns a paginated list of prescription records for a specific prescriber. " +
                    "Requires ROLE_ADMIN or ROLE_USER.",
            tags = {"Prescription Records"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of records"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public Page<RecordResponse> getByUserId(
            @Parameter(description = "ID of the prescriber (user)", example = "1", required = true)
            @PathVariable Long userId,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return service.getByUserId(userId, page, size);
    }

    // PUT /api/v1/records/{id}
    @PutMapping("/{id}")
    @Operation(
            summary = "Update a prescription record",
            description = "Replaces a record's diagnosis, date, and ALL entries in one transaction. " +
                    "User and client associations are immutable after creation. " +
                    "Requires ROLE_ADMIN or ROLE_USER.",
            tags = {"Prescription Records"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Record updated successfully",
                    content = @Content(schema = @Schema(implementation = RecordResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Record not found")
    })
    public RecordResponse update(
            @Parameter(description = "ID of the prescription record", example = "1", required = true)
            @PathVariable Long id,
            @Parameter(description = "Record update request body", required = true)
            @Valid @RequestBody UpdateRecordRequest request) {
        return service.update(id, request);
    }

    // DELETE /api/v1/records/{id}  →  204 No Content
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete a prescription record",
            description = "Deletes a prescription record and all its entries. " +
                    "Requires ROLE_ADMIN. ROLE_USER will receive 403 Forbidden.",
            tags = {"Prescription Records"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Record deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - DELETE requires ROLE_ADMIN"),
            @ApiResponse(responseCode = "404", description = "Record not found")
    })
    public void delete(
            @Parameter(description = "ID of the prescription record", example = "1", required = true)
            @PathVariable Long id) {
        service.delete(id);
    }
}
