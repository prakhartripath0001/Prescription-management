-- V4: Create app_users table for Spring Security authentication
-- ─────────────────────────────────────────────────────────────────────────────
-- This table stores application login accounts (separate from the `users` table,
-- which stores prescribers/doctors as business entities).
-- Default accounts are created by DataInitializer.java on first startup.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE app_users (
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    username VARCHAR(50)  NOT NULL,
    password VARCHAR(100) NOT NULL COMMENT 'BCrypt-hashed password (strength 12)',
    role     VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER'
                          COMMENT 'ROLE_ADMIN = full access | ROLE_USER = read/write, no delete',
    enabled  BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_app_users          PRIMARY KEY (id),
    CONSTRAINT uk_app_users_username UNIQUE      (username),
    CONSTRAINT chk_app_users_role    CHECK       (role IN ('ROLE_ADMIN', 'ROLE_USER')),

    INDEX idx_app_users_username (username)
) ENGINE = InnoDB;

