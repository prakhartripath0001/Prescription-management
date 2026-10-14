package com.prescription_Management_REST_API.controller;

import com.prescription_Management_REST_API.dto.request.CreateRecordRequest;
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

    public PrescriptionRecordController(
            PrescriptionRecordService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecordResponse create(
            @Valid @RequestBody CreateRecordRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public RecordResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public Page<RecordResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.getAll(page, size);
    }
}
