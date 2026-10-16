package com.prescription_Management_REST_API.security;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) configuration for the Prescription Management REST API.
 *
 * Registers:
 *  - API info (title, version, description)
 *  - HTTP Basic Auth security scheme (required on all /api/v1/records/** endpoints)
 *  - Server URL and API tags
 *
 * Swagger UI is available at: http://localhost:8080/swagger-ui/index.html
 * OpenAPI JSON is available at: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI prescriptionManagementOpenAPI() {
        return new OpenAPI()
                .addServersItem(new Server().url("http://localhost:8080").description("Local development server"))
                .info(new Info()
                        .title("Prescription Management REST API")
                        .version("1.0.0")
                        .description("""
                                REST API for managing prescription records.
                                
                                ## Authentication
                                All endpoints require HTTP Basic Authentication.
                                
                                Default accounts (seeded on startup):
                                - admin / admin123  →  ROLE_ADMIN (full access incl. DELETE)
                                - doctor1 / doctor123 → ROLE_USER (GET, POST, PUT only)
                                
                                ## Role-Based Access Control
                                - ROLE_ADMIN: GET, POST, PUT, DELETE on /api/v1/records/**
                                - ROLE_USER:  GET, POST, PUT on /api/v1/records/** (DELETE → 403)
                                """))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
                .securitySchemes(List.of(
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .name("basicAuth")
                                .description("HTTP Basic Authentication (username:password)")
                ))
                .tags(List.of(
                        new Tag().name("Prescription Records").description("CRUD operations for prescription records")
                ));
    }
}
