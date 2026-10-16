package com.prescription_Management_REST_API.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Spring Security configuration for the Prescription Management REST API.
 *
 * Authentication: HTTP Basic Auth backed by the {@code app_users} DB table.
 * Authorization (role-based):
 *
 *  ROLE_ADMIN → GET, POST, PUT, DELETE on /api/v1/records/**
 *  ROLE_USER  → GET, POST, PUT           on /api/v1/records/**
 *                                            (DELETE → 403 Forbidden)
 *
 * All 401 / 403 responses are returned as structured JSON (never HTML).
 * Sessions are stateless — no cookies, no CSRF token needed.
 *
 * Latest-feature notes (Java 21 / Spring Boot 4):
 *  - Lambda DSL for all HttpSecurity configuration (no deprecated chained .and())
 *  - PasswordEncoder with BCrypt strength 12 (recommended for Java 21)
 *  - Virtual threads enabled in application.properties (spring.threads.virtual.enabled=true)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AppUserDetailsService userDetailsService;

    public SecurityConfig(AppUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ── REST API: disable CSRF (no browser session, no cookies) ────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── Stateless: no HTTP session created or used ─────────────────────
            .sessionManagement(sm ->
                    sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── Load users from DB ─────────────────────────────────────────────
            .userDetailsService(userDetailsService)

            // ── Role-based endpoint authorization ─────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // DELETE is restricted to ADMIN only
                .requestMatchers(HttpMethod.DELETE, "/api/v1/records/**")
                    .hasRole("ADMIN")
                // All other record endpoints are accessible to both roles
                .requestMatchers(HttpMethod.GET,  "/api/v1/records/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.POST, "/api/v1/records/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.PUT,  "/api/v1/records/**").hasAnyRole("ADMIN", "USER")
                // Reject every other path — no accidental open endpoints
                .anyRequest().denyAll()
            )

            // ── HTTP Basic authentication with JSON 401 ────────────────────────
            .httpBasic(basic -> basic
                    .authenticationEntryPoint(SecurityConfig::handleUnauthorized))

            // ── JSON 403 for role violations ───────────────────────────────────
            .exceptionHandling(ex -> ex
                    .accessDeniedHandler(SecurityConfig::handleForbidden));

        return http.build();
    }

    /**
     * BCrypt with strength 12 — the recommended minimum for Java 21 production use.
     * Higher strength = more computation per hash = slower brute-force attacks.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ─── JSON error writers ────────────────────────────────────────────────────

    /** 401 — missing or wrong credentials. Returns JSON, never HTML. */
    private static void handleUnauthorized(HttpServletRequest req,
                                           HttpServletResponse res,
                                           Exception ex) throws IOException {
        writeJson(res, 401, "Unauthorized",
                "Authentication required. Provide valid Basic Auth credentials " +
                "(Authorization: Basic base64(username:password)).");
    }

    /** 403 — authenticated but insufficient role for this action. */
    private static void handleForbidden(HttpServletRequest req,
                                        HttpServletResponse res,
                                        Exception ex) throws IOException {
        writeJson(res, 403, "Forbidden",
                "You do not have permission to perform this action. " +
                "DELETE requires the ADMIN role.");
    }

    private static void writeJson(HttpServletResponse response,
                                  int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                """
                {"status":%d,"error":"%s","message":"%s","timestamp":"%s","fieldErrors":null}
                """.formatted(status, error, message, LocalDateTime.now()).strip()
        );
    }
}

