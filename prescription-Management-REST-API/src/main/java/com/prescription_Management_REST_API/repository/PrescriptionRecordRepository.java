package com.prescription_Management_REST_API.repository;

import com.prescription_Management_REST_API.entity.PrescriptionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PrescriptionRecordRepository
        extends JpaRepository<PrescriptionRecord, Long> {

    @EntityGraph(attributePaths = "entries")
    Optional<PrescriptionRecord> findWithEntriesById(Long id);

    @EntityGraph(attributePaths = "entries")
    Page<PrescriptionRecord> findAllBy(Pageable pageable);
}
