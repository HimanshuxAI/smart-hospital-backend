package com.smarthospital.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("smart-hospital.security.refresh-token")
@Validated
public record RefreshTokenProperties(
        @NotNull(message = "Refresh token expiration must be configured")
        Duration expiration) {

    @AssertTrue(message = "Refresh token expiration must be positive")
    public boolean isExpirationPositive() {
        return expiration == null || expiration.compareTo(Duration.ZERO) > 0;
    }
}
