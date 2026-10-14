package com.prescription_Management_REST_API.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Entity
@Table(name = "record_entries")
public class RecordEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 150)
    @Column(name = "entry_name", nullable = false, length = 150)
    private String entryName;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String frequency;

    @NotNull
    @Positive
    @Column(name = "duration_days", nullable = false)
    private Integer duration;

    @Size(max = 1000)
    @Column(length = 1000)
    private String instructions;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false)
    private PrescriptionRecord prescriptionRecord;

    protected RecordEntry() {
    }

    public RecordEntry(String entryName, BigDecimal quantity,
                       String frequency, Integer duration,
                       String instructions) {
        this.entryName = entryName;
        this.quantity = quantity;
        this.frequency = frequency;
        this.duration = duration;
        this.instructions = instructions;
    }

    void setPrescriptionRecord(PrescriptionRecord prescriptionRecord) {
        this.prescriptionRecord = prescriptionRecord;
    }

    public Long getId() {
        return id;
    }

    public String getEntryName() {
        return entryName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getFrequency() {
        return frequency;
    }

    public Integer getDuration() {
        return duration;
    }

    public String getInstructions() {
        return instructions;
    }

    public PrescriptionRecord getPrescriptionRecord() {
        return prescriptionRecord;
    }
}
