package com.prescription_Management_REST_API;

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
import com.prescription_Management_REST_API.service.PrescriptionRecordService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionRecordServiceTest {

    @Mock
    private PrescriptionRecordRepository recordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private PrescriptionRecordService service;

    private User user(Long id) {
        User u = new User("Dr. Alice Johnson", "REG-2024-001", "+1-555-100-0001");
        try {
            java.lang.reflect.Field f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return u;
    }

    private Client client(Long id) {
        Client c = new Client("James Wilson", LocalDate.of(1985, 3, 12), "+1-555-200-0001");
        try {
            java.lang.reflect.Field f = Client.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(c, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return c;
    }

    private CreateRecordRequest createRequest(Long userId, Long clientId) {
        return new CreateRecordRequest(
                userId,
                clientId,
                LocalDate.of(2026, 10, 16),
                "Seasonal flu with mild fever",
                List.of(
                        new RecordEntryRequest("Paracetamol 500mg", BigDecimal.valueOf(2.0), "Twice daily", 5, "Take after meals")
                )
        );
    }

    private UpdateRecordRequest updateRequest() {
        return new UpdateRecordRequest(
                LocalDate.of(2026, 10, 16),
                "Updated diagnosis",
                List.of(
                        new RecordEntryRequest("Paracetamol 500mg", BigDecimal.valueOf(2.0), "Twice daily", 5, "Take after meals")
                )
        );
    }

    // ── CREATE tests ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Create record successfully with new idempotency key")
    void create_withNewIdempotencyKey_shouldCreateRecord() {
        CreateRecordRequest req = createRequest(1L, 1L);
        User u = user(1L);
        Client c = client(1L);
        PrescriptionRecord saved = record(100L, u, c);
        saved.setIdempotencyKey("test-key-123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(c));
        when(recordRepository.findByIdempotencyKey("test-key-123")).thenReturn(Optional.empty());
        when(recordRepository.save(any(PrescriptionRecord.class))).thenReturn(saved);

        RecordResponse response = service.create(req, "test-key-123");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.clientId()).isEqualTo(1L);
        verify(recordRepository, times(1)).save(any(PrescriptionRecord.class));
    }

    @Test
    @DisplayName("Create record returns existing record when idempotency key already used")
    void create_withExistingIdempotencyKey_shouldReturnExistingRecord() {
        CreateRecordRequest req = createRequest(1L, 1L);
        User u = user(1L);
        Client c = client(1L);
        PrescriptionRecord existing = record(50L, u, c);
        existing.setIdempotencyKey("dup-key");

        when(recordRepository.findByIdempotencyKey("dup-key")).thenReturn(Optional.of(existing));

        RecordResponse response = service.create(req, "dup-key");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(50L);
        verify(recordRepository, never()).save(any(PrescriptionRecord.class));
    }

    @Test
    @DisplayName("Create record without idempotency key works normally")
    void create_withoutIdempotencyKey_shouldCreateRecord() {
        CreateRecordRequest req = createRequest(1L, 1L);
        User u = user(1L);
        Client c = client(1L);
        PrescriptionRecord saved = record(200L, u, c);

        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(c));
        when(recordRepository.save(any(PrescriptionRecord.class))).thenReturn(saved);

        RecordResponse response = service.create(req, null);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(200L);
        verify(recordRepository, times(1)).save(any(PrescriptionRecord.class));
    }

    @Test
    @DisplayName("Create record with non-existent user throws EntityNotFoundException")
    void create_withNonExistentUser_shouldThrow() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(createRequest(99L, 1L), null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Create record with non-existent client throws EntityNotFoundException")
    void create_withNonExistentClient_shouldThrow() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(createRequest(1L, 99L), null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Client not found");
    }

    @Test
    @DisplayName("Create record with empty entries throws IllegalArgumentException")
    void create_withEmptyEntries_shouldThrow() {
        CreateRecordRequest req = new CreateRecordRequest(
                1L, 1L, LocalDate.of(2026, 10, 16), "Diagnosis", List.of()
        );
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client(1L)));
        assertThatThrownBy(() -> service.create(req, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one prescription entry is required");
    }

    private PrescriptionRecord record(Long id, User user, Client client) {
        PrescriptionRecord r = new PrescriptionRecord(user, client, LocalDate.of(2026, 10, 16), "Seasonal flu");
        try {
            java.lang.reflect.Field f = PrescriptionRecord.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(r, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        r.addEntry(new RecordEntry("Paracetamol 500mg", BigDecimal.valueOf(2.0), "Twice daily", 5, "Take after meals"));
        return r;
    }

    // ── GET BY ID tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("Get existing record by ID returns record")
    void getById_existingRecord_shouldReturnRecord() {
        User u = user(1L);
        Client c = client(1L);
        PrescriptionRecord r = record(1L, u, c);
        when(recordRepository.findWithEntriesById(1L)).thenReturn(Optional.of(r));

        RecordResponse response = service.getById(1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.diagnosis()).isEqualTo("Seasonal flu");
    }

    @Test
    @DisplayName("Get non-existent record by ID throws EntityNotFoundException")
    void getById_nonExistentRecord_shouldThrow() {
        when(recordRepository.findWithEntriesById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Record not found");
    }

    // ── GET ALL tests ────────────────────────────────────────────────────

    @Test
    @DisplayName("Get all records returns paginated page")
    void getAll_shouldReturnPaginatedPage() {
        User u = user(1L);
        Client c = client(1L);
        PrescriptionRecord r = record(1L, u, c);
        Page<PrescriptionRecord> page = new PageImpl<>(List.of(r), PageRequest.of(0, 20), 1);
        when(recordRepository.findAllBy(PageRequest.of(0, 20))).thenReturn(page);

        Page<RecordResponse> result = service.getAll(0, 20);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(1L);
    }

    // ── UPDATE tests ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Update existing record replaces diagnosis and entries")
    void update_existingRecord_shouldReplaceFields() {
        User u = user(1L);
        Client c = client(1L);
        PrescriptionRecord r = record(1L, u, c);
        when(recordRepository.findWithEntriesById(1L)).thenReturn(Optional.of(r));
        when(recordRepository.save(any(PrescriptionRecord.class))).thenReturn(r);

        RecordResponse response = service.update(1L, updateRequest());

        assertThat(response).isNotNull();
        assertThat(response.diagnosis()).isEqualTo("Updated diagnosis");
    }

    @Test
    @DisplayName("Update non-existent record throws EntityNotFoundException")
    void update_nonExistentRecord_shouldThrow() {
        when(recordRepository.findWithEntriesById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(999L, updateRequest()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Update with empty entries throws IllegalArgumentException")
    void update_withEmptyEntries_shouldThrow() {
        UpdateRecordRequest emptyEntries = new UpdateRecordRequest(
                LocalDate.of(2026, 10, 16), "Diagnosis", List.of()
        );
        assertThatThrownBy(() -> service.update(1L, emptyEntries))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one prescription entry is required");
    }

    // ── DELETE tests ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Delete existing record succeeds")
    void delete_existingRecord_shouldSucceed() {
        when(recordRepository.existsById(1L)).thenReturn(true);
        doNothing().when(recordRepository).deleteById(1L);

        service.delete(1L);

        verify(recordRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Delete non-existent record throws EntityNotFoundException")
    void delete_nonExistentRecord_shouldThrow() {
        when(recordRepository.existsById(999L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Record not found");
    }
}