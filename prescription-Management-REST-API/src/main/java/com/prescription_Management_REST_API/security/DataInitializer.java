package com.prescription_Management_REST_API.security;

import com.prescription_Management_REST_API.entity.AppUser;
import com.prescription_Management_REST_API.entity.enums.Role;
import com.prescription_Management_REST_API.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds default security accounts into the {@code app_users} table on startup
 * if they do not already exist.
 *
 * Passwords are BCrypt-encoded at runtime using the configured {@link PasswordEncoder}
 * (strength 12), so no plain-text or pre-computed hashes appear in source code or SQL.
 *
 * Default accounts:
 *  ┌──────────┬──────────────┬────────────┬────────────────────────────────────┐
 *  │ username │ password     │ role       │ permissions                        │
 *  ├──────────┼──────────────┼────────────┼────────────────────────────────────┤
 *  │ admin    │ admin123     │ ROLE_ADMIN │ GET, POST, PUT, DELETE             │
 *  │ doctor1  │ doctor123    │ ROLE_USER  │ GET, POST, PUT  (no DELETE)        │
 *  └──────────┴──────────────┴────────────┴────────────────────────────────────┘
 *
 * Change these credentials before deploying to any environment.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder   passwordEncoder;

    public DataInitializer(AppUserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        createIfAbsent("admin",   "admin123",  Role.ROLE_ADMIN);
        createIfAbsent("doctor1", "doctor123", Role.ROLE_USER);
    }

    private void createIfAbsent(String username, String rawPassword, Role role) {
        if (!userRepository.existsByUsername(username)) {
            userRepository.save(
                    new AppUser(username, passwordEncoder.encode(rawPassword), role, true));
            log.info("Created default {} account: '{}'", role, username);
        }
    }
}

