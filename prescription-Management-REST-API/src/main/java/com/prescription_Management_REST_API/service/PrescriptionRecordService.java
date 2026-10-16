package com.prescription_Management_REST_API.service;

import com.prescription_Management_REST_API.dto.request.CreateRecordRequest;
import com.prescription_Management_REST_API.dto.request.RecordEntryRequest;
import com.prescription_Management_REST_API.dto.request.UpdateRecordRequest;
import com.prescription_Management_REST_API.dto.response.RecordResponse;
import com.prescription_Management_REST_API.entity.Client;
import com.prescription_Management_REST_API.entity.PrescriptionRecord;
import com.prescription_Management_REST_API.entity.RecordEntry;
import com.prescription_Management_REST_API.entity.User;
import com.prescription_Management_REST_API.repository.ClientRepository;
import com.prescription_Management_REST_API.repository.PrescriptionRecordRepository;
import com.prescription_Management_REST_API.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Business-logic layer for prescription records.
 *
 * Validation contract:
 *  - Mandatory fields are enforced by @Valid on DTOs (controller layer).
 *  - Reference integrity (user/client existence) is checked before any write.
 *  - At least one entry per record is enforced here (not on the JPA entity).
 *  - Idempotency: callers may supply an X-Idempotency-Key header.
 *    If the key was seen before, the original record is returned — no duplicate is created.
 *  - All writes are wrapped in @Transactional — record + entries succeed or fail together.
 */
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

    // ─────────────────────────────────────────────────────────────── CREATE ───
    /**
     * Creates a new prescription record together with all its entries in a single
     * transaction. If {@code idempotencyKey} is supplied and already stored,
     * the existing record is returned without creating a duplicate.
     *
     * @param request        validated request body
     * @param idempotencyKey optional UUID from X-Idempotency-Key header (may be null)
     */
    @Transactional
    public RecordResponse create(CreateRecordRequest request, String idempotencyKey) {

        // ── Idempotency check ────────────────────────────────────────────────
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<PrescriptionRecord> existing =
                    recordRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                // Key already used — return the original record, do not create a duplicate
                return toResponse(existing.get());
            }
        }

        // ── Reference integrity checks ───────────────────────────────────────
        User user = userRepository.findById(request.userId())
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found: id=" + request.userId()));

        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Client not found: id=" + request.clientId()));

        // ── Business rule: at least one entry required ───────────────────────
        if (request.entries() == null || request.entries().isEmpty()) {
            throw new IllegalArgumentException("At least one prescription entry is required.");
        }

        // ── Build record + entries (single transaction) ──────────────────────
        PrescriptionRecord record = new PrescriptionRecord(
                user, client, request.recordDate(), request.diagnosis());

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            record.setIdempotencyKey(idempotencyKey.trim());
        }

        for (RecordEntryRequest item : request.entries()) {
            record.addEntry(toEntry(item));
        }

        // recordRepository.save() persists the record AND all entries together.
        // If any entry fails (e.g. constraint violation) the whole transaction rolls back.
        return toResponse(recordRepository.save(record));
    }

    // ──────────────────────────────────────────────────────────────── GET ONE ─
    @Transactional(readOnly = true)
    public RecordResponse getById(Long id) {
        return toResponse(findWithEntries(id));
    }

    // ──────────────────────────────────────────────────────────────── GET ALL ─
    @Transactional(readOnly = true)
    public Page<RecordResponse> getAll(int page, int size) {
        validatePage(page, size);
        return recordRepository
                .findAllBy(pageRequest(page, size))
                .map(this::toResponse);
    }

    // ─────────────────────────────────────────────────────── GET BY CLIENT ID ─
    @Transactional(readOnly = true)
    public Page<RecordResponse> getByClientId(Long clientId, int page, int size) {
        validatePage(page, size);
        // Reference check: 404 if client doesn't exist
        if (!clientRepository.existsById(clientId)) {
            throw new EntityNotFoundException("Client not found: id=" + clientId);
        }
        return recordRepository
                .findByClientId(clientId, pageRequest(page, size))
                .map(this::toResponse);
    }

    // ──────────────────────────────────────────────────────── GET BY USER ID ──
    @Transactional(readOnly = true)
    public Page<RecordResponse> getByUserId(Long userId, int page, int size) {
        validatePage(page, size);
        // Reference check: 404 if user doesn't exist
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found: id=" + userId);
        }
        return recordRepository
                .findByUserId(userId, pageRequest(page, size))
                .map(this::toResponse);
    }

    // ─────────────────────────────────────────────────────────────── UPDATE ───
    /**
     * Replaces a record's diagnosis, date, and ALL entries in one transaction.
     * User and client associations are immutable after creation.
     */
    @Transactional
    public RecordResponse update(Long id, UpdateRecordRequest request) {
        if (request.entries() == null || request.entries().isEmpty()) {
            throw new IllegalArgumentException("At least one prescription entry is required.");
        }

        PrescriptionRecord record = findWithEntries(id);

        record.setRecordDate(request.recordDate());
        record.setDiagnosis(request.diagnosis());

        // Clear old entries (orphanRemoval=true deletes them from DB),
        // then add the replacement set — all in one transaction.
        record.clearEntries();
        for (RecordEntryRequest item : request.entries()) {
            record.addEntry(toEntry(item));
        }

        // Entity is managed — Hibernate flushes changes automatically at commit.
        return toResponse(record);
    }

    // ─────────────────────────────────────────────────────────────── DELETE ───
    @Transactional
    public void delete(Long id) {
        if (!recordRepository.existsById(id)) {
            throw new EntityNotFoundException("Record not found: id=" + id);
        }
        // Cascades to record_entries via ON DELETE CASCADE in DB + orphanRemoval in JPA
        recordRepository.deleteById(id);
    }

    // ─────────────────────────────────────────────────────────────── HELPERS ──

    private PrescriptionRecord findWithEntries(Long id) {
        return recordRepository.findWithEntriesById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Record not found: id=" + id));
    }

    private static RecordEntry toEntry(RecordEntryRequest item) {
        return new RecordEntry(
                item.entryName(),
                item.quantity(),
                item.frequency(),
                item.duration(),
                item.instructions()
        );
    }

    private RecordResponse toResponse(PrescriptionRecord record) {
        List<RecordResponse.EntryResponse> entries =
                record.getEntries().stream()
                        .map(e -> new RecordResponse.EntryResponse(
                                e.getId(),
                                e.getEntryName(),
                                e.getQuantity(),
                                e.getFrequency(),
                                e.getDuration(),
                                e.getInstructions()
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

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page must be >= 0 and size must be between 1 and 100.");
        }
    }

    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by("id").descending());
    }
}
