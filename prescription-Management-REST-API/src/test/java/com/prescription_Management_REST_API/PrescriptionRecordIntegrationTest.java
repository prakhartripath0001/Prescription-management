package com.prescription_Management_REST_API;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prescription_Management_REST_API.dto.request.CreateRecordRequest;
import com.prescription_Management_REST_API.dto.request.RecordEntryRequest;
import com.prescription_Management_REST_API.dto.request.UpdateRecordRequest;
import com.prescription_Management_REST_API.entity.enums.Role;
import com.prescription_Management_REST_API.repository.AppUserRepository;
import com.prescription_Management_REST_API.repository.ClientRepository;
import com.prescription_Management_REST_API.repository.PrescriptionRecordRepository;
import com.prescription_Management_REST_API.repository.UserRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Requires Docker daemon — run manually in an environment with Docker available")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PrescriptionRecordIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("prescription_management_system")
            .withUsername("root")
            .withPassword("root");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PrescriptionRecordRepository recordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String ADMIN_AUTH = "Basic " +
            java.util.Base64.getEncoder().encodeToString("admin:admin123".getBytes());
    private static final String USER_AUTH = "Basic " +
            java.util.Base64.getEncoder().encodeToString("doctor1:doctor123".getBytes());

    @BeforeEach
    void setUp() {
        recordRepository.deleteAll();
        appUserRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();

        appUserRepository.save(new com.prescription_Management_REST_API.entity.AppUser(
                "admin", passwordEncoder.encode("admin123"), Role.ROLE_ADMIN, true));
        appUserRepository.save(new com.prescription_Management_REST_API.entity.AppUser(
                "doctor1", passwordEncoder.encode("doctor123"), Role.ROLE_USER, true));
    }

    private HttpHeaders authHeaders(String auth) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", auth);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    @Test
    @DisplayName("Integration: POST then GET record as ADMIN returns 201 then 200")
    void endToEnd_createAndGetRecord_asAdmin() throws Exception {
        com.prescription_Management_REST_API.entity.User u = new com.prescription_Management_REST_API.entity.User(
                "Dr. Alice", "REG-001", "+1-555-0001");
        userRepository.save(u);
        com.prescription_Management_REST_API.entity.Client c = new com.prescription_Management_REST_API.entity.Client(
                "John Doe", LocalDate.of(1980, 1, 1), "+1-555-0002");
        clientRepository.save(c);

        String json = objectMapper.writeValueAsString(new CreateRecordRequest(
                u.getId(), c.getId(),
                LocalDate.of(2026, 10, 16),
                "Seasonal flu",
                List.of(new RecordEntryRequest("Paracetamol 500mg", BigDecimal.valueOf(2.0), "Twice daily", 5, "Take after meals"))
        ));

        ResponseEntity<String> post = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/records",
                HttpMethod.POST,
                new HttpEntity<>(json, authHeaders(ADMIN_AUTH)),
                String.class
        );
        assertThat(post.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(post.getBody()).contains("Seasonal flu");

        String body = post.getBody();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(body);
        m.find();
        Long recordId = Long.parseLong(m.group(1));

        ResponseEntity<String> get = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/records/" + recordId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(ADMIN_AUTH)),
                String.class
        );
        assertThat(get.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(get.getBody()).contains("Seasonal flu");
    }

    @Test
    @DisplayName("Integration: DELETE as USER returns 403, as ADMIN returns 204")
    void endToEnd_delete_roleBasedAccess() throws Exception {
        com.prescription_Management_REST_API.entity.User u = new com.prescription_Management_REST_API.entity.User(
                "Dr. Alice", "REG-001", "+1-555-0001");
        userRepository.save(u);
        com.prescription_Management_REST_API.entity.Client c = new com.prescription_Management_REST_API.entity.Client(
                "John Doe", LocalDate.of(1980, 1, 1), "+1-555-0002");
        clientRepository.save(c);

        String json = objectMapper.writeValueAsString(new CreateRecordRequest(
                u.getId(), c.getId(),
                LocalDate.of(2026, 10, 16),
                "Seasonal flu",
                List.of(new RecordEntryRequest("Paracetamol 500mg", BigDecimal.valueOf(2.0), "Twice daily", 5, "Take after meals"))
        ));

        ResponseEntity<String> post = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/records",
                HttpMethod.POST,
                new HttpEntity<>(json, authHeaders(ADMIN_AUTH)),
                String.class
        );
        assertThat(post.getStatusCode().is2xxSuccessful()).isTrue();

        String body = post.getBody();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(body);
        m.find();
        Long recordId = Long.parseLong(m.group(1));

        // DELETE as USER -> 403
        ResponseEntity<String> deleteAsUser = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/records/" + recordId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(USER_AUTH)),
                String.class
        );
        assertThat(deleteAsUser.getStatusCode().is4xxClientError()).isTrue();
        assertThat(deleteAsUser.getStatusCode().value()).isEqualTo(403);

        // Record still exists
        ResponseEntity<String> getAfter = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/records/" + recordId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(ADMIN_AUTH)),
                String.class
        );
        assertThat(getAfter.getStatusCode().is2xxSuccessful()).isTrue();

        // DELETE as ADMIN -> 204
        ResponseEntity<String> deleteAsAdmin = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/records/" + recordId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(ADMIN_AUTH)),
                String.class
        );
        assertThat(deleteAsAdmin.getStatusCode().value()).isEqualTo(204);

        // Record is gone
        ResponseEntity<String> getGone = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/records/" + recordId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(ADMIN_AUTH)),
                String.class
        );
        assertThat(getGone.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("Integration: UPDATE record as ADMIN returns 200 with new diagnosis")
    void endToEnd_updateRecord_asAdmin() throws Exception {
        com.prescription_Management_REST_API.entity.User u = new com.prescription_Management_REST_API.entity.User(
                "Dr. Alice", "REG-001", "+1-555-0001");
        userRepository.save(u);
        com.prescription_Management_REST_API.entity.Client c = new com.prescription_Management_REST_API.entity.Client(
                "John Doe", LocalDate.of(1980, 1, 1), "+1-555-0002");
        clientRepository.save(c);

        String json = objectMapper.writeValueAsString(new CreateRecordRequest(
                u.getId(), c.getId(),
                LocalDate.of(2026, 10, 16),
                "Original diagnosis",
                List.of(new RecordEntryRequest("Paracetamol 500mg", BigDecimal.valueOf(2.0), "Twice daily", 5, "Take after meals"))
        ));

        ResponseEntity<String> post = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/records",
                HttpMethod.POST,
                new HttpEntity<>(json, authHeaders(ADMIN_AUTH)),
                String.class
        );
        String body = post.getBody();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(body);
        m.find();
        Long recordId = Long.parseLong(m.group(1));

        String updateJson = objectMapper.writeValueAsString(new UpdateRecordRequest(
                LocalDate.of(2026, 10, 17),
                "Updated diagnosis",
                List.of(new RecordEntryRequest("Vitamin C 1000mg", BigDecimal.valueOf(1.0), "Once daily", 7, "Take with food"))
        ));

        ResponseEntity<String> update = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/records/" + recordId,
                HttpMethod.PUT,
                new HttpEntity<>(updateJson, authHeaders(ADMIN_AUTH)),
                String.class
        );
        assertThat(update.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(update.getBody()).contains("Updated diagnosis");
        assertThat(update.getBody()).contains("Vitamin C 1000mg");
    }
}