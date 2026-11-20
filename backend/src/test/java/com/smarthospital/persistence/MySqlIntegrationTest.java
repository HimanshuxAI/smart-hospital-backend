package com.smarthospital.persistence;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

public abstract class MySqlIntegrationTest {

    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("smarthospital")
            .withUsername("smarthospital")
            .withPassword("smarthospital-test-password");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    protected static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> MYSQL.getJdbcUrl()
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.security.user.name", () -> "integration-test-user");
        registry.add("spring.security.user.password", () -> "integration-test-password");
        registry.add("smart-hospital.security.jwt.secret",
                () -> "integration-test-jwt-secret-with-at-least-32-characters");
        registry.add("smart-hospital.security.jwt.access-token-expiration", () -> "PT15M");
        registry.add("smart-hospital.security.refresh-token.expiration", () -> "P7D");
        registry.add("spring.main.banner-mode", () -> "off");
    }
}
