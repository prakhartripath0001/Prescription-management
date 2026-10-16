# Prescription Management REST API — Implementation Strategy

This document describes the design and implementation approach for the Prescription Management REST API.

---

## 1. Entity Relationships & Database Schema

### Entities

| Entity | Table | Purpose |
|--------|-------|---------|
| `User` | `users` | Prescriber/doctor (business entity) |
| `Client` | `clients` | Patient |
| `PrescriptionRecord` | `prescription_records` | Prescription record (header) |
| `RecordEntry` | `record_entries` | Medication line item within a record |
| `AppUser` | `app_users` | Security/authentication account |

### Relationships

- **User → PrescriptionRecord**: One-to-Many (prescriber creates many records)
- **Client → PrescriptionRecord**: One-to-Many (patient has many records)
- **PrescriptionRecord → RecordEntry**: One-to-Many with cascade delete + orphan removal
- **AppUser**: Standalone security entity, no business relationship to `User`

### Flyway Migrations

| Migration | Description |
|-----------|-------------|
| `V1__create_tables.sql` | Creates all 4 business tables with PKs, FKs, indexes, check constraints |
| `V2__seed_test_data.sql` | Seeds 3 prescribers, 4 clients, 4 records, 10 entries |
| `V3__add_idempotency_key.sql` | Adds `idempotency_key` column + unique constraint |
| `V4__create_app_users.sql` | Creates `app_users` table for Spring Security |

Schema DDL owned by Flyway (`spring.jpa.hibernate.ddl-auto=validate`). Hibernate validates schema-entity consistency only.

### Package Structure

```
com.prescription_Management_REST_API
├── controller          # REST controllers (Spring MVC)
├── dto
│   ├── request         # Input DTOs (Java records with validation)
│   └── response        # Output DTOs (Java records, never entities)
├── entity              # JPA entities (@Entity, @Table)
│   └── enums           # RecordStatus, Role
├── exception           # GlobalExceptionHandler (@RestControllerAdvice)
├── repository          # Spring Data JPA repositories
├── security            # SecurityConfig, UserDetailsService, DataInitializer, OpenApiConfig
└── service             # Business logic (@Service, @Transactional)
```

## 2. Validation & Transactional Business Logic

### Validation Layers

1. **Controller layer**: `@Valid` on `@RequestBody` DTOs — catches malformed requests early (400)
2. **DTO layer**: Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Positive`, `@DecimalMin`, `@Size`, `@PastOrPresent`, `@Pattern`)
3. **Service layer**: Business-rule validation not expressible as annotations:
   - At least one entry per record (enforced in `create()` and `update()`)
   - Reference integrity (user/client existence checked before write)
   - Idempotency key deduplication

### Transaction Management

- `@Transactional` on all write operations (`create`, `update`, `delete`)
- Read-only `@Transactional(readOnly = true)` on `loadUserByUsername`
- All-or-nothing semantics: record + entries succeed or fail together
- `spring.jpa.open-in-view=false` — no lazy loading outside transaction boundary

## 3. REST Endpoints & Pagination

### Endpoints

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| POST | `/api/v1/records` | ADMIN, USER | Create a record (idempotency key support) |
| GET | `/api/v1/records/{id}` | ADMIN, USER | Get a single record |
| GET | `/api/v1/records` | ADMIN, USER | List all records (paginated) |
| GET | `/api/v1/records/client/{id}` | ADMIN, USER | List records by client (paginated) |
| GET | `/api/v1/records/user/{id}` | ADMIN, USER | List records by prescriber (paginated) |
| PUT | `/api/v1/records/{id}` | ADMIN, USER | Update a record (replaces all entries) |
| DELETE | `/api/v1/records/{id}` | ADMIN only | Delete a record |

### Pagination

- `Page<RecordResponse>` for all list endpoints
- Configurable `page` (0-based) and `size` (1–100)
- Default: `page=0, size=20`
- Sorted by `id` descending

## 4. Spring Security & Role-Based Access Control

### Authentication

- **HTTP Basic Auth** (stateless, no sessions, no CSRF)
- Users stored in `app_users` DB table
- **BCrypt password encoding** (strength 12)
- Custom `AppUserDetailsService` implements `UserDetailsService`, loads from DB
- `DataInitializer` seeds default accounts on startup:
  - `admin` / `admin123` → `ROLE_ADMIN`
  - `doctor1` / `doctor123` → `ROLE_USER`

### Authorization

- `ROLE_ADMIN`: GET, POST, PUT, DELETE on all `/api/v1/records/**`
- `ROLE_USER`: GET, POST, PUT on `/api/v1/records/**` (DELETE → 403 Forbidden)
- All 401/403 responses return structured JSON (never HTML)

## 5. Testing Strategy

### Unit Tests (JUnit 5 + Mockito)

**`PrescriptionRecordServiceTest`** — 12 test cases covering create, get, update, delete with success and failure paths.

**`PrescriptionRecordControllerTest`** — 8 test cases (@WebMvcTest + MockMvc) covering all HTTP methods with ADMIN/USER roles and unauthenticated access.

### Integration Tests (Spring Boot Test + Testcontainers MySQL)

**`PrescriptionRecordIntegrationTest`** — 3 end-to-end tests using a real MySQL 8.0 container:
- POST then GET as ADMIN
- DELETE role-based access (USER → 403, ADMIN → 204)
- UPDATE as ADMIN

Requires Docker.

## 6. API Documentation (OpenAPI / Swagger)

- **Library**: `springdoc-openapi-starter-webmvc-ui` 2.7.0
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
- **Security scheme**: HTTP Basic Auth applied globally
- **Annotations**: `@Operation`, `@ApiResponses`, `@Parameter`, `@Tag`, `@SecurityRequirement`

## 7. Running the Application

### Prerequisites

- Java 21 LTS
- Maven 3.x
- MySQL database (integration tests require Docker)

### Commands

```bash
# Build
./mvnw compile

# Run unit tests only (no Docker needed)
./mvnw test -Dtest='*ServiceTest,*ControllerTest'

# Run all tests including integration (requires Docker)
./mvnw test

# Run application
./mvnw spring-boot:run
```

### Curl Tests

See `test-data/curl-commands.txt` for comprehensive curl test commands.

