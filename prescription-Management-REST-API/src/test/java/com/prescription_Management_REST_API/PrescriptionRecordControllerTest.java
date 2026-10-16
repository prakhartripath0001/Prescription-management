package com.prescription_Management_REST_API;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prescription_Management_REST_API.dto.request.CreateRecordRequest;
import com.prescription_Management_REST_API.dto.request.RecordEntryRequest;
import com.prescription_Management_REST_API.dto.request.UpdateRecordRequest;
import com.prescription_Management_REST_API.dto.response.RecordResponse;
import com.prescription_Management_REST_API.service.PrescriptionRecordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = com.prescription_Management_REST_API.controller.PrescriptionRecordController.class)
class PrescriptionRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PrescriptionRecordService service;

    private String jsonCreate() {
        try {
            return objectMapper.writeValueAsString(new CreateRecordRequest(
                    1L, 1L,
                    LocalDate.of(2026, 10, 16),
                    "Seasonal flu with mild fever",
                    List.of(new RecordEntryRequest("Paracetamol 500mg", BigDecimal.valueOf(2.0), "Twice daily", 5, "Take after meals"))
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("POST /api/v1/records as ADMIN returns 201")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void create_asAdmin_shouldReturn201() throws Exception {
        RecordResponse response = new RecordResponse(
                1L, 1L, 1L, LocalDate.of(2026, 10, 16), "Seasonal flu",
                com.prescription_Management_REST_API.entity.enums.RecordStatus.DRAFT,
                List.of(new RecordResponse.EntryResponse(1L, "Paracetamol 500mg", BigDecimal.valueOf(2.0), "Twice daily", 5, "Take after meals"))
        );
        when(service.create(any(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/v1/records")
                        .header("X-Idempotency-Key", "test-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCreate()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.diagnosis").value("Seasonal flu"));
    }

    @Test
    @DisplayName("GET /api/v1/records/1 as USER returns 200")
    @WithMockUser(username = "doctor1", roles = "USER")
    void getById_asUser_shouldReturn200() throws Exception {
        RecordResponse response = new RecordResponse(
                1L, 1L, 1L, LocalDate.of(2026, 10, 16), "Seasonal flu",
                com.prescription_Management_REST_API.entity.enums.RecordStatus.DRAFT,
                List.of()
        );
        when(service.getById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/records/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/records (paginated) returns 200")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAll_shouldReturn200() throws Exception {
        Page<RecordResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(service.getAll(anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/v1/records?page=0&size=20"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/records/1 as USER returns 403")
    @WithMockUser(username = "doctor1", roles = "USER")
    void delete_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/v1/records/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/records/1 as ADMIN returns 204")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_asAdmin_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/records/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST without authentication returns 401")
    void create_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCreate()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/v1/records/1 as ADMIN returns 200")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void update_asAdmin_shouldReturn200() throws Exception {
        RecordResponse response = new RecordResponse(
                1L, 1L, 1L, LocalDate.of(2026, 10, 16), "Updated diagnosis",
                com.prescription_Management_REST_API.entity.enums.RecordStatus.DRAFT,
                List.of()
        );
        when(service.update(anyLong(), any())).thenReturn(response);

        String json = objectMapper.writeValueAsString(new UpdateRecordRequest(
                LocalDate.of(2026, 10, 16), "Updated diagnosis",
                List.of(new RecordEntryRequest("Paracetamol 500mg", BigDecimal.valueOf(2.0), "Twice daily", 5, "Take after meals"))
        ));
        mockMvc.perform(put("/api/v1/records/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("Updated diagnosis"));
    }

    @Test
    @DisplayName("GET /api/v1/records/999 returns 404")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getById_notFound_shouldReturn404() throws Exception {
        when(service.getById(999L))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("Record not found: id=999"));

        mockMvc.perform(get("/api/v1/records/999"))
                .andExpect(status().isNotFound());
    }
}