package com.prescription_Management_REST_API.controller;

import com.prescription_Management_REST_API.dto.request.CreateRecordRequest;
import com.prescription_Management_REST_API.dto.request.UpdateRecordRequest;
import com.prescription_Management_REST_API.dto.response.RecordResponse;
import com.prescription_Management_REST_API.service.PrescriptionRecordService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/records")
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
    public RecordResponse create(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateRecordRequest request) {
        return service.create(request, idempotencyKey);
    }

    // GET /api/v1/records/{id}
    @GetMapping("/{id}")
    public RecordResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    // GET /api/v1/records?page=0&size=20
    @GetMapping
    public Page<RecordResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.getAll(page, size);
    }

    // GET /api/v1/records/client/{clientId}?page=0&size=20
    @GetMapping("/client/{clientId}")
    public Page<RecordResponse> getByClientId(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.getByClientId(clientId, page, size);
    }

    // GET /api/v1/records/user/{userId}?page=0&size=20
    @GetMapping("/user/{userId}")
    public Page<RecordResponse> getByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.getByUserId(userId, page, size);
    }

    // PUT /api/v1/records/{id}
    @PutMapping("/{id}")
    public RecordResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRecordRequest request) {
        return service.update(id, request);
    }

    // DELETE /api/v1/records/{id}  →  204 No Content
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
