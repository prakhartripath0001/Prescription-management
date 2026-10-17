# Prescription Management REST API

A Spring Boot REST API for managing prescription records with role-based access control, idempotency, and database migrations via Flyway.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Database Schema & Migrations](#database-schema--migrations)
- [Setup & Run](#setup--run)
- [Authentication & Security](#authentication--security)
- [API Documentation](#api-documentation)
- [Sample API Requests](#sample-api-requests)
- [Running Tests](#running-tests)
- [Architecture Decisions](#architecture-decisions)

---

## Features

- **CRUD operations** for prescription records with medication entries
- **Pagination** on all list endpoints (configurable page/size)
- **Idempotency** via `X-Idempotency-Key` header (prevents duplicate creation on retry)
- **HTTP Basic Authentication** backed by database-stored accounts
- **Role-Based Access Control**: `ROLE_ADMIN` (full access) / `ROLE_USER` (no DELETE)
- **Structured JSON error responses** (never leaks stack traces or DB internals)
- **Flyway migrations** for versioned schema management
- **Java 21 virtual threads** for high I/O throughput
- **OpenAPI/Swagger** documentation at `/swagger-ui/index.html`

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 LTS |
| Framework | Spring Boot 3.2.5 |
| Data Access | Spring Data JPA, Hibernate |
| Database | MySQL 8.x |
| Migrations | Flyway |
| Security | Spring Security (HTTP Basic, BCrypt) |
| Validation | Jakarta Bean Validation |
| API Docs | SpringDoc OpenAPI 2.5.0 |
| Testing | JUnit 5, Mockito, MockMvc, Testcontainers |
| Build | Apache Maven |

## Project Structure

```
src/main/java/com/prescription_Management_REST_API/
├── controller/          # REST controllers (@RestController)
├── dto/
│   ├── request/         # Input DTOs (Java records + validation)
│   └── response/        # Output DTOs (Java records, never entities)
├── entity/              # JPA entities (@Entity, @Table)
│   └── enums/           # RecordStatus, Role
├── exception/           # Global exception handler (@RestControllerAdvice)
├── repository/          # Spring Data JPA repositories
├── security/            # Security config, UserDetailsService, DataInitializer, OpenApiConfig
├── service/             # Business logic (@Service, @Transactional)
└── PrescriptionManagementRestApiApplication.java

src/main/resources/
├── application.properties
└── db/migration/        # Flyway SQL migrations (V1-V4)

src/test/java/
├── PrescriptionRecordServiceTest.java      # Unit tests (Mockito)
├── PrescriptionRecordControllerTest.java   # Controller tests (MockMvc)
├── PrescriptionRecordIntegrationTest.java  # Integration tests (Testcontainers MySQL)
└── PrescriptionManagementRestApiApplicationTests.java

test-data/curl-commands.txt   # Sample curl test commands
docs/                         # Design and strategy documentation
```
---
## Database Schema & Migrations

Four Flyway migration SQL files under `src/main/resources/db/migration/`:

| Migration | Description |
|-----------|-------------|
| `V1__create_tables.sql` | Creates `users`, `clients`, `prescription_records`, `record_entries` with PKs, FKs, indexes, check constraints |
| `V2__seed_test_data.sql` | Seeds 3 prescribers, 4 clients, 4 records, 10 entries |
| `V3__add_idempotency_key.sql` | Adds `idempotency_key` column + unique constraint for dedup |
| `V4__create_app_users.sql` | Creates `app_users` table for Spring Security authentication |

Schema DDL is owned by Flyway (`spring.jpa.hibernate.ddl-auto=validate`). Hibernate validates schema-entity consistency at startup only.

---
## Setup & Run

### Prerequisites

- Java 21 LTS
- Apache Maven 3.x
- MySQL 8.x database (or Docker for integration tests)

### 1. Configure Database

Create a MySQL database:

```sql
CREATE DATABASE prescription_management_system;
```

Edit `src/main/resources/application.properties` or set environment variables:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/prescription_management_system
spring.datasource.username=your_username
spring.datasource.password=your_password
```

Or use environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`

### 2. Build

```bash
./mvnw compile
```

### 3. Run the Application

```bash
./mvnw spring-boot:run
```

The API will be available at: `http://localhost:8080`

---
## Authentication & Security

### Authentication Method

**HTTP Basic Auth** — send `Authorization: Basic base64(username:password)` header on every request.

### Default Accounts (seeded on startup)

| Username | Password | Role | Permissions |
|----------|----------|------|-------------|
| `admin` | `admin123` | `ROLE_ADMIN` | GET, POST, PUT, DELETE |
| `doctor1` | `doctor123` | `ROLE_USER` | GET, POST, PUT (DELETE -> 403) |

### Role-Based Access Control

| Endpoint | ROLE_ADMIN | ROLE_USER |
|----------|------------|-----------|
| `GET /api/v1/records` | Yes | Yes |
| `GET /api/v1/records/{id}` | Yes | Yes |
| `GET /api/v1/records/client/{id}` | Yes | Yes |
| `GET /api/v1/records/user/{id}` | Yes | Yes |
| `POST /api/v1/records` | Yes | Yes |
| `PUT /api/v1/records/{id}` | Yes | Yes |
| `DELETE /api/v1/records/{id}` | Yes | No (403 Forbidden) |

### Password Security

- Passwords are BCrypt-hashed (strength 12) before storage
- Default accounts are created at runtime via `DataInitializer` — no plain-text passwords in source code
- Stateless sessions — no cookies, no CSRF tokens

---
## API Documentation

### Swagger UI

Once the application is running, open:

```
http://localhost:8080/swagger-ui/index.html
```

### OpenAPI JSON

```
http://localhost:8080/v3/api-docs
```

All endpoints require HTTP Basic Auth. Use the "Authorize" button in Swagger UI with your credentials.

## Sample API Requests

---

### Base URL

```
http://localhost:8080/api/v1/records
```

### 1. Create a Record (POST)

```bash
curl.exe -X POST http://localhost:8080/api/v1/records ^
  -u admin:admin123 ^
  -H "Content-Type: application/json" ^
  -H "X-Idempotency-Key: 11111111-1111-1111-1111-111111111111" ^
  -d "{\"userId\":1,\"clientId\":1,\"recordDate\":\"2026-10-16\",\"diagnosis\":\"Seasonal flu with mild fever\",\"entries\":[{\"entryName\":\"Paracetamol 500mg\",\"quantity\":2.0,\"frequency\":\"Twice daily\",\"duration\":5,\"instructions\":\"Take after meals\"}]}"
```

Expected: `201 Created`

### 2. Get a Record by ID (GET)

```bash
curl.exe -X GET http://localhost:8080/api/v1/records/1 -u admin:admin123
```

Expected: `200 OK`

### 3. Get All Records (Paginated)

```bash
curl.exe -X GET "http://localhost:8080/api/v1/records?page=0&size=20" -u admin:admin123
```

Expected: `200 OK` with Page wrapper

### 4. Update a Record (PUT)

```bash
curl.exe -X PUT http://localhost:8080/api/v1/records/1 ^
  -u admin:admin123 ^
  -H "Content-Type: application/json" ^
  -d "{\"recordDate\":\"2026-10-16\",\"diagnosis\":\"Updated diagnosis\",\"entries\":[{\"entryName\":\"Paracetamol 500mg\",\"quantity\":2.0,\"frequency\":\"Twice daily\",\"duration\":5,\"instructions\":\"Take after meals\"}]}"
```

Expected: `200 OK`

### 5. Delete a Record (DELETE) — ADMIN only

```bash
curl.exe -X DELETE http://localhost:8080/api/v1/records/1 -u admin:admin123
```

Expected: `204 No Content`

### 6. DELETE as ROLE_USER (should fail)

```bash
curl.exe -X DELETE http://localhost:8080/api/v1/records/1 -u doctor1:doctor123
```

Expected: `403 Forbidden`

### 7. Request without Authentication (should fail)

```bash
curl.exe -X POST http://localhost:8080/api/v1/records ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\":1,\"clientId\":1,\"recordDate\":\"2026-10-16\",\"diagnosis\":\"Test\",\"entries\":[{\"entryName\":\"Test\",\"quantity\":1.0,\"frequency\":\"Once daily\",\"duration\":3,\"instructions\":null}]}"
```

Expected: `401 Unauthorized`

For comprehensive curl commands including negative test cases, see `test-data/curl-commands.txt`.

## Running Tests

### Unit Tests Only (no Docker required)

```bash
./mvnw test -Dtest='*ServiceTest,*ControllerTest'
```

### All Tests Including Integration (Docker required)

Integration tests use Testcontainers to spin up a real MySQL 8.0 container:

```bash
./mvnw test
```

### Test Coverage

| Test Class | Type | Test Cases |
|------------|------|------------|
| `PrescriptionRecordServiceTest` | Unit (Mockito) | 14 — create, get, update, delete with success/failure paths |
| `PrescriptionRecordControllerTest` | Controller (MockMvc) | 8 — all HTTP methods with ADMIN/USER roles and unauthenticated access |
| `PrescriptionRecordIntegrationTest` | Integration (Testcontainers MySQL) | 3 — end-to-end: POST->GET, DELETE role-based, UPDATE |

## Architecture Decisions

### 1. DTOs Instead of Exposing Entities

Controllers return Java record DTOs (`RecordResponse`, `ErrorResponse`) — never JPA entities. This prevents leaking internal fields (e.g., `idempotency_key`) and allows the API to evolve independently of the database schema.

### 2. Flyway Owns DDL

`spring.jpa.hibernate.ddl-auto=validate` — Flyway migrations own all schema DDL. Hibernate only validates schema-entity consistency at startup. This is production-safe and provides an auditable schema history.

### 3. Stateless HTTP Basic Auth

No sessions, no CSRF, no JWT. Credentials sent on every request via `Authorization: Basic` header. Suitable for REST APIs consumed by curl, mobile apps, and scripts over HTTPS.

### 4. Separate User (Business) and AppUser (Security)

`User` table = prescriber/doctor business entity. `AppUser` table = security login account. Authentication and business identity are separate concerns.

### 5. Transaction Management

- `@Transactional` on all write operations (`create`, `update`, `delete`) — all-or-nothing semantics
- `@Transactional(readOnly = true)` on `loadUserByUsername`
- `spring.jpa.open-in-view=false` — no lazy loading outside transaction boundary

### 6. Idempotency

`X-Idempotency-Key` header on POST requests. Server checks for existing key before creating. Database unique constraint acts as last line of defense against race conditions.

### 7. Global Exception Handling

`@RestControllerAdvice` converts all exceptions to structured `ErrorResponse` JSON. Internal details (stack traces, SQL messages) are never sent to clients.

## Additional Documentation

| File | Contents |
|------|----------|
| `docs/IMPLEMENTATION_STRATEGY.md` | Full implementation strategy — entities, packages, validation, security, testing, OpenAPI, run commands |
| `docs/DESIGN_TRADE_OFFS.md` | 10 design decisions with pros/cons tables and verdicts |
| `docs/RESULTS_REPORT.md` | Test execution metrics, environment details, and verification results report |
| `test-data/curl-commands.txt` | Comprehensive curl test commands (happy path, negative tests, idempotency, role-based access) |

---