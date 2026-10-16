package com.prescription_Management_REST_API.entity;

import com.prescription_Management_REST_API.entity.enums.Role;
import jakarta.persistence.*;

/**
 * Security / authentication account — separate from the {@link User} entity
 * which represents a business prescriber/doctor.
 *
 * Stored in the {@code app_users} table (created by V4 Flyway migration).
 * Default accounts are seeded by {@code DataInitializer} on first startup.
 */
@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    /** BCrypt-hashed password. Never stored or returned in plain text. */
    @Column(nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    /** Required by JPA. */
    protected AppUser() {}

    public AppUser(String username, String password, Role role, boolean enabled) {
        this.username = username;
        this.password = password;
        this.role     = role;
        this.enabled  = enabled;
    }

    public Long getId()          { return id; }
    public String getUsername()  { return username; }
    public String getPassword()  { return password; }
    public Role getRole()        { return role; }
    public boolean isEnabled()   { return enabled; }
}

