
-- V1: Create the MediScript database schema

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    registration_number VARCHAR(50) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_user_registration_number
        UNIQUE (registration_number)
) ENGINE=InnoDB;

CREATE TABLE clients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    date_of_birth DATE NULL,
    phone_number VARCHAR(20) NOT NULL,

    CONSTRAINT pk_clients PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE prescription_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    record_date DATE NOT NULL,
    diagnosis VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    CONSTRAINT pk_prescription_records PRIMARY KEY (id),

    CONSTRAINT fk_record_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_record_client
        FOREIGN KEY (client_id) REFERENCES clients(id),

    CONSTRAINT chk_record_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED')),

    INDEX idx_record_user (user_id),
    INDEX idx_record_client (client_id),
    INDEX idx_record_date (record_date)
) ENGINE=InnoDB;

CREATE TABLE record_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    record_id BIGINT NOT NULL,
    entry_name VARCHAR(150) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    frequency VARCHAR(100) NOT NULL,
    duration_days INT NOT NULL,
    instructions VARCHAR(1000) NULL,

    CONSTRAINT pk_record_entries PRIMARY KEY (id),

    CONSTRAINT fk_entry_record
        FOREIGN KEY (record_id)
        REFERENCES prescription_records(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_entry_quantity CHECK (quantity > 0),
    CONSTRAINT chk_entry_duration CHECK (duration_days > 0),

    INDEX idx_entry_record (record_id)
) ENGINE=InnoDB;
