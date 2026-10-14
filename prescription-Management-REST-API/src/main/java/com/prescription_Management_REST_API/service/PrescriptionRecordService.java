package com.prescription_Management_REST_API.service;

import com.prescription_Management_REST_API.dto.request.CreateRecordRequest;
import com.prescription_Management_REST_API.dto.request.RecordEntryRequest;
import com.prescription_Management_REST_API.dto.response.RecordResponse;
import com.prescription_Management_REST_API.entity.Client;
import com.prescription_Management_REST_API.entity.PrescriptionRecord;
import com.prescription_Management_REST_API.entity.RecordEntry;
import com.prescription_Management_REST_API.entity.User;
import com.prescription_Management_REST_API.repository.ClientRepository;
import com.prescription_Management_REST_API.repository.PrescriptionRecordRepository;
import com.prescription_Management_REST_API.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PrescriptionRecordService {

    private final PrescriptionRecordRepository recordRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    public PrescriptionRecordService(
            PrescriptionRecordRepository recordRepository,
            UserRepository userRepository,
            ClientRepository clientRepository) {
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional
    public RecordResponse create(CreateRecordRequest request) {
        if (request.entries() == null || request.entries().isEmpty()) {
            throw new IllegalArgumentException("At least one entry is required");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found: id=" + request.userId()));

        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Client not found: id=" + request.clientId()));

        PrescriptionRecord record = new PrescriptionRecord(
                user,
                client,
                request.recordDate(),
                request.diagnosis()
        );

        for (RecordEntryRequest item : request.entries()) {
            record.addEntry(new RecordEntry(
                    item.entryName(),
                    item.quantity(),
                    item.frequency(),
                    item.duration(),
                    item.instructions()
            ));
        }

        return toResponse(recordRepository.save(record));
    }

    @Transactional(readOnly = true)
    public RecordResponse getById(Long id) {
        PrescriptionRecord record =
                recordRepository.findWithEntriesById(id)
                        .orElseThrow(() ->
                                new EntityNotFoundException("Record not found: id=" + id));

        return toResponse(record);
    }

    @Transactional(readOnly = true)
    public Page<RecordResponse> getAll(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page must be non-negative and size must be between 1 and 100");
        }

        return recordRepository.findAllBy(
                PageRequest.of(page, size, Sort.by("id").descending())
        ).map(this::toResponse);
    }

    private RecordResponse toResponse(PrescriptionRecord record) {
        List<RecordResponse.EntryResponse> entries =
                record.getEntries().stream()
                        .map(entry -> new RecordResponse.EntryResponse(
                                entry.getId(),
                                entry.getEntryName(),
                                entry.getQuantity(),
                                entry.getFrequency(),
                                entry.getDuration(),
                                entry.getInstructions()
                        ))
                        .toList();

        return new RecordResponse(
                record.getId(),
                record.getUser().getId(),
                record.getClient().getId(),
                record.getRecordDate(),
                record.getDiagnosis(),
                record.getStatus(),
                entries
        );
    }
}
