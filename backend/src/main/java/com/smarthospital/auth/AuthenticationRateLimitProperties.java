package com.smarthospital.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("smart-hospital.security.rate-limit")
@Validated
public record AuthenticationRateLimitProperties(
        @NotNull(message = "Authentication rate-limit window must be configured")
        Duration window,
        @Valid
        @NotNull(message = "Login rate limits must be configured")
        Login login,
        @Valid
        @NotNull(message = "Registration rate limits must be configured")
        Registration registration,
        @Valid
        @NotNull(message = "Refresh rate limits must be configured")
        Refresh refresh,
        @Min(value = 1, message = "Authentication rate-limit tracked-entry capacity must be positive")
        @Max(value = 1000000, message = "Authentication rate-limit tracked-entry capacity must not exceed 1000000")
        int maxTrackedEntries) {

    @AssertTrue(message = "Authentication rate-limit window must be at least one millisecond")
    public boolean isWindowPositive() {
        return window == null || (window.compareTo(Duration.ZERO) > 0 && window.toMillis() > 0);
    }

    public record Login(
            @Min(value = 1, message = "Login account rate limit must be positive")
            int accountLimit,
            @Min(value = 1, message = "Login IP rate limit must be positive")
            int ipLimit) {
    }

    public record Registration(
            @Min(value = 1, message = "Registration account rate limit must be positive")
            int accountLimit,
            @Min(value = 1, message = "Registration IP rate limit must be positive")
            int ipLimit) {
    }

    public record Refresh(
            @Min(value = 1, message = "Refresh token rate limit must be positive")
            int tokenLimit,
            @Min(value = 1, message = "Refresh IP rate limit must be positive")
            int ipLimit) {
    }
}
