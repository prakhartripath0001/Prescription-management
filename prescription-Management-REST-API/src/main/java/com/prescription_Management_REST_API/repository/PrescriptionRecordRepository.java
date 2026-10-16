package com.prescription_Management_REST_API.repository;

import com.prescription_Management_REST_API.entity.PrescriptionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrescriptionRecordRepository
        extends JpaRepository<PrescriptionRecord, Long> {

    // Load a single record eagerly with its entries
    @EntityGraph(attributePaths = "entries")
    Optional<PrescriptionRecord> findWithEntriesById(Long id);

    // Paginated list — all records, entries eagerly loaded
    @EntityGraph(attributePaths = "entries")
    Page<PrescriptionRecord> findAllBy(Pageable pageable);

    // Get all records belonging to a specific client
    @EntityGraph(attributePaths = "entries")
    Page<PrescriptionRecord> findByClientId(Long clientId, Pageable pageable);

    // Get all records created by a specific user
    @EntityGraph(attributePaths = "entries")
    Page<PrescriptionRecord> findByUserId(Long userId, Pageable pageable);

    // Idempotency check — find an existing record by the client-supplied deduplication key
    @EntityGraph(attributePaths = "entries")
    Optional<PrescriptionRecord> findByIdempotencyKey(String idempotencyKey);
}
