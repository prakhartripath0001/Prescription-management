package com.prescription_Management_REST_API.entity;

import com.prescription_Management_REST_API.entity.enums.RecordStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prescription_records")
public class PrescriptionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @NotNull
    @PastOrPresent
    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @NotBlank
    @Size(max = 2000)
    @Column(nullable = false, length = 2000)
    private String diagnosis;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    private RecordStatus status = RecordStatus.DRAFT;

    /**
     * Optional client-supplied deduplication key (UUID).
     * If provided on creation, subsequent requests with the same key
     * return the existing record instead of creating a duplicate.
     * Enforced as UNIQUE at the DB level (see V3 migration).
     */
    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    // NOTE: @Size(min=1) and @Valid are NOT effective on JPA-managed @OneToMany collections.
    // Enforce "at least one entry" in the service layer before saving.
    @OneToMany(
            mappedBy = "prescriptionRecord",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<RecordEntry> entries = new ArrayList<>();

    protected PrescriptionRecord() {
    }

    public PrescriptionRecord(User user, Client client,
                              LocalDate recordDate,
                              String diagnosis) {
        this.user = user;
        this.client = client;
        this.recordDate = recordDate;
        this.diagnosis = diagnosis;
        this.status = RecordStatus.DRAFT;
    }

    public void addEntry(RecordEntry entry) {
        entries.add(entry);
        entry.setPrescriptionRecord(this);
    }

    public void removeEntry(RecordEntry entry) {
        entries.remove(entry);
        entry.setPrescriptionRecord(null);
    }

    /** Clears all entries so they can be replaced on update.
     *  orphanRemoval=true ensures removed entries are deleted from DB. */
    public void clearEntries() {
        entries.forEach(e -> e.setPrescriptionRecord(null));
        entries.clear();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Client getClient() {
        return client;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public RecordStatus getStatus() {
        return status;
    }

    public void setStatus(RecordStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public List<RecordEntry> getEntries() {
        return List.copyOf(entries);
    }
}
