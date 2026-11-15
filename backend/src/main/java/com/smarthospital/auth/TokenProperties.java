package com.smarthospital.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("smart-hospital.security.jwt")
@Validated
public record TokenProperties(
        @NotBlank(message = "JWT secret must not be blank")
        @Size(min = 32, message = "JWT secret must contain at least 32 characters")
        String secret,
        @NotNull(message = "Access token expiration must be configured")
        Duration accessTokenExpiration) {

    private static final Pattern UNRESOLVED_PLACEHOLDER = Pattern.compile("\\$\\{[^}]+}");

    @AssertTrue(message = "JWT secret must be configured")
    public boolean isSecretConfigured() {
        return secret != null && !secret.isBlank() && !UNRESOLVED_PLACEHOLDER.matcher(secret).find();
    }
}
