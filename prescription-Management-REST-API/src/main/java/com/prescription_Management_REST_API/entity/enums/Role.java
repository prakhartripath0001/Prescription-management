package com.prescription_Management_REST_API.entity.enums;

/**
 * Application security roles.
 *
 * ROLE_ADMIN  – Full access: read, create, update, delete all records.
 * ROLE_USER   – Restricted: read, create, update only. Cannot delete.
 *
 * The "ROLE_" prefix is required by Spring Security's hasRole() checks.
 */
public enum Role {
    ROLE_ADMIN,
    ROLE_USER
}

