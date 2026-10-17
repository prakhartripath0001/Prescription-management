PRESCRIPTION MANAGEMENT REST API
PROJECT COMPLETION CHECKLIST

Instructions:
Mark each item as [x] only after implementing and verifying it.

1. DATA MODEL AND DATABASE
[ ] User entity created with ID, name, registration number and phone number
[ ] Client entity created with ID, name, date of birth or age, and phone number
[ ] PrescriptionRecord entity created with ID, user, client, record date, diagnosis and status
[ ] RecordEntry entity created with entry name, quantity, frequency, duration and instructions
[ ] One-to-many relationship between records and entries works correctly
[ ] Many-to-one relationships from records to users and clients are implemented
[ ] Primary keys, foreign keys, NOT NULL and unique constraints are configured
[ ] Flyway migration creates all tables on a fresh database
[ ] JPA schema validation succeeds against the migrated schema

2. PROJECT ARCHITECTURE
[ ] Controller package created
[ ] Service package created
[ ] Repository package created
[ ] DTO request and response packages created
[ ] Entity and mapper packages created
[ ] Centralized exception-handling package created
[ ] Configuration and security packages created
[ ] API responses use DTOs instead of exposing persistence entities

3. REST APIs AND PAGINATION
[ ] POST /api/v1/records creates a record with multiple entries
[ ] GET /api/v1/records/{id} retrieves a record and its entries
[ ] GET /api/v1/records supports pagination
[ ] PUT /api/v1/records/{id} updates a record
[ ] DELETE /api/v1/records/{id} deletes a record according to defined rules
[ ] GET /api/v1/records/client/{clientId} filters records by client
[ ] GET /api/v1/records/user/{userId} filters records by user
[ ] Appropriate HTTP status codes are returned
[ ] Invalid pagination parameters are rejected

4. VALIDATION AND BUSINESS LOGIC
[ ] Mandatory fields are validated
[ ] Invalid or nonexistent user and client IDs are rejected
[ ] Quantity must be positive and valid
[ ] Frequency is validated
[ ] Duration must be positive and within defined limits
[ ] Record dates are validated
[ ] At least one entry is required when creating a record
[ ] Record creation and entry insertion use one transaction
[ ] Transaction rollback is tested when an entry fails
[ ] Idempotency key or documented duplicate-prevention strategy is implemented

5. API ERRORS AND EXCEPTION HANDLING
[ ] Validation errors return HTTP 400 with consistent JSON
[ ] Missing records return HTTP 404 with consistent JSON
[ ] Unauthenticated requests return HTTP 401
[ ] Unauthorized roles return HTTP 403
[ ] Duplicate or conflicting requests have defined error responses
[ ] Internal database errors do not leak to API clients
[ ] Stack traces and sensitive details are not exposed in responses

6. SECURITY
[ ] Spring Security is configured
[ ] Basic authentication is implemented
[ ] Roles and permissions are defined
[ ] Protected endpoints require authentication
[ ] Authorization checks prevent unauthorized record access
[ ] Passwords are stored using a secure password encoder
[ ] Database credentials and secrets are excluded from Git
[ ] Security behavior is tested

7. AUTOMATED TESTING
[ ] Service unit tests cover successful creation
[ ] Unit tests cover missing users, clients and records
[ ] Request validation tests cover invalid fields
[ ] Controller tests cover expected HTTP status codes
[ ] Integration tests use a real MySQL test database or Testcontainers
[ ] Database constraints and entity relationships are tested
[ ] Transaction rollback is tested
[ ] Authentication and role-based authorization are tested
[ ] Flyway migrations are tested on a fresh database
[ ] All automated tests pass

8. API DOCUMENTATION AND SUBMISSION
[ ] Swagger/OpenAPI documentation is available
[ ] Postman collection is included
[ ] Sample request and response JSON files are included
[ ] README explains prerequisites and setup commands
[ ] README documents environment variables and database setup
[ ] Fresh-clone startup instructions have been tested
[ ] GitHub repository contains source code and tests
[ ] GitHub repository access is configured for the evaluator
[ ] Architecture and design trade-offs are documented
[ ] Results report includes actual test results
[ ] AI tools, purposes and usage levels are disclosed accurately
[ ] Actual time spent and external assistance are documented

RECOMMENDED ORDER OF WORK
1. Finish the schema and migrations; verify all four tables and foreign keys.
2. Complete record CRUD APIs: create, retrieve, list, update and delete.
3. Add DTO validation and centralized error handling.
4. Implement transactions and idempotency.
5. Finish authentication and authorization.
6. Write and run unit and integration tests.
7. Finish Swagger, Postman, README and submission evidence.

FINAL VERIFICATION
[ ] Run the complete test suite and record the actual result
[ ] Verify the application starts from a fresh database
[ ] Verify the documented commands work in a clean environment
[ ] Review the repository for secrets before sharing it