# Prescription Management REST API — Design Trade-offs

This document explains significant design decisions and their trade-offs.

---

## 1. Flyway Owns DDL, Hibernate Validates Only

### Decision
`spring.jpa.hibernate.ddl-auto=validate` — Flyway migrations own all schema DDL. Hibernate does not create, alter, or drop tables.

### Trade-offs

| Pros | Cons |
|------|------|
| Schema versioning is explicit and auditable via SQL migrations | Schema changes require writing and reviewing SQL migration files |
| Production-safe: no accidental `drop-and-create` on startup | Two sources of truth to keep in sync (entities + migration SQL) |
| Works consistently across environments (dev, test, prod) | Migration must be written before entity changes take effect |
| Clear audit trail of schema evolution | Manual effort to write SQL for every schema change |

**Verdict**: Correct choice for production. `validate` mode catches entity-schema mismatches at startup rather than at runtime.

## 2. DTOs (Java Records) Instead of Exposing JPA Entities

### Decision
Controllers and services operate on Java record DTOs (`CreateRecordRequest`, `RecordResponse`, etc.). JPA entities are never returned directly to clients.

### Trade-offs

| Pros | Cons |
|------|------|
| Clients never see internal entity structure (idempotency keys, internal fields) | More boilerplate: DTO classes + mapping code in service layer |
| Can evolve API independently of database schema | Mapping between entities and DTOs is manual (no MapStruct) |
| Validation annotations on DTOs, not entities | Slight performance overhead from mapping |
| Clean separation between API contract and persistence model | |

**Verdict**: Correct choice. API stability and security outweigh the mapping boilerplate.

## 3. Stateless HTTP Basic Auth (No Sessions, No CSRF, No JWT)

### Decision
Stateless REST API using HTTP Basic Auth with Spring Security's `SessionCreationPolicy.STATELESS`. CSRF is disabled.

### Trade-offs

| Pros | Cons |
|------|------|
| Simple: no session management, no token storage, no refresh tokens | Credentials sent on every request (slightly higher overhead) |
| No CSRF token needed (appropriate for non-browser clients) | Credentials in memory on client side; must use HTTPS in production |
| Works naturally with curl, mobile apps, and script clients | No built-in token revocation |
| No JWT signing/verification overhead | Basic Auth credentials are base64-encoded, not encrypted |

**Verdict**: Correct for an internal REST API consumed by trusted clients over HTTPS. For public-facing APIs, consider OAuth2 or JWT.

## 4. Separate `User` (Business) and `AppUser` (Security) Entities

### Decision
`User` table = prescriber/doctor business entity. `AppUser` table = security login account. They are completely separate.

### Trade-offs

| Pros | Cons |
|------|------|
| Security concerns separated from business domain | Two tables to manage instead of one |
| A prescriber can exist without login credentials (and vice versa) | Slightly more complex data model |
| Security roles stored independently of prescriber data | Application code must manage two entity types |
| Business data can be shared across security accounts | |

**Verdict**: Correct for separation of concerns. Authentication and business identity are different concepts.

## 5. Idempotency via Client-Supplied UUID Header

### Decision
`X-Idempotency-Key` header on POST requests. Server checks for existing key before creating. DB unique constraint as last line of defense.

### Trade-offs

| Pros | Cons |
|------|------|
| Safe retries after network failures | Clients must generate and track UUIDs |
| Database unique constraint prevents race conditions | Additional header parsing and lookup per create request |
| Idempotency key is optional — clients can opt out | Slightly more complex service logic |
| Works well with distributed retry mechanisms | |

**Verdict**: Correct for a REST API where network retries are expected. The DB unique constraint is the critical safety net.

## 6. Pagination with `Page<RecordResponse>` (Capped at Size 100)

### Decision
All list endpoints return `Page<RecordResponse>`. Max page size capped at 100 to prevent oversized responses.

### Trade-offs

| Pros | Cons |
|------|------|
| Prevents clients from requesting unbounded result sets | Clients needing >100 records must make multiple requests |
| Predictable memory usage on server | Pagination metadata adds response overhead |
| Standard Spring Data pattern, well understood | Offset-based pagination can be slow on very large tables |
| Sort order is deterministic (by id descending) | |

**Verdict**: Correct. Size cap protects against denial-of-service via oversized queries.

## 7. Java 21 Virtual Threads

### Decision
`spring.threads.virtual.enabled=true` — uses Project Loom virtual threads for all web request handling.

### Trade-offs

| Pros | Cons |
|------|------|
| Dramatically improved throughput under I/O-bound workloads | Virtual threads are stable in Java 21 |
| No code changes required to benefit | Some thread-local-heavy libraries may not be virtual-thread-friendly |
| Dramatically reduced memory footprint per request | Debugging virtual thread stacks can be different |
| Ideal for database-backed REST APIs (I/O bound) | |

**Verdict**: Correct for Java 21. No downside for this workload.

## 8. Global Exception Handler (`@RestControllerAdvice`)

### Decision
Centralized error handling that converts all exceptions to structured `ErrorResponse` JSON. Internal details (stack traces, SQL messages) are never sent to clients.

### Trade-offs

| Pros | Cons |
|------|------|
| Consistent error response format across all endpoints | Must maintain exhaustive list of exception handlers |
| No information leakage (no stack traces, SQL errors) | Some exceptions may need custom handling not anticipated |
| Client-friendly error messages with field-level validation details | Slight abstraction layer between exception and response |
| Logging done server-side for diagnostics | |

**Verdict**: Correct. Security and consistency are paramount for a production API.

## 9. Testcontainers with MySQL for Integration Tests

### Decision
Integration tests spin up a real MySQL 8.0 Docker container and run full end-to-end API calls.

### Trade-offs

| Pros | Cons |
|------|------|
| Tests run against real database, not H2 mock | Requires Docker to be installed and running |
| Catches database-specific issues (SQL dialect, constraints) | Slower test execution (container startup time) |
| Tests Flyway migrations against real MySQL | Not suitable for CI environments without Docker |
| True end-to-end validation of JPA + Flyway + Security | |

**Verdict**: Correct for catching integration issues. Unit tests (Mockito) cover logic; integration tests cover database interaction.

## 10. Spring Boot Version

### Decision
The original `pom.xml` referenced Spring Boot `4.1.1`, which does not exist as a released version. Changed to `3.5.0` (latest available).

### Trade-offs

| Pros | Cons |
|------|------|
| Real, released Spring Boot version with working dependency management | If 3.5.0 is not the intended version, dependency versions may differ |
| All existing code is compatible with Spring Boot 3.x (`jakarta.*`, lambda DSL, etc.) | May need adjustment if a specific Spring Boot version is mandated |

**Verdict**: Necessary fix for buildability. Code is Spring Boot 3.x-compatible.
