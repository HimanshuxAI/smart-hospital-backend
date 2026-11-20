package com.smarthospital.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("smart-hospital.security.refresh-token")
@Validated
public record RefreshTokenProperties(
        @NotNull(message = "Refresh token expiration must be configured")
        Duration expiration,
        @NotNull(message = "Refresh token family lifetime must be configured")
        Duration familyLifetime,
        @NotNull(message = "Refresh token cleanup retention must be configured")
        Duration cleanupRetention,
        @NotBlank(message = "Refresh token cleanup cron must be configured")
        String cleanupCron,
        @Min(value = 1, message = "Refresh token cleanup batch size must be positive")
        @Max(value = 10000, message = "Refresh token cleanup batch size must not exceed 10000")
        int cleanupBatchSize) {

    @ConstructorBinding
    public RefreshTokenProperties(
            Duration expiration,
            Duration familyLifetime,
            Duration cleanupRetention,
            String cleanupCron,
            int cleanupBatchSize) {
        this.expiration = expiration;
        this.familyLifetime = familyLifetime;
        this.cleanupRetention = cleanupRetention;
        this.cleanupCron = cleanupCron;
        this.cleanupBatchSize = cleanupBatchSize;
    }

    public RefreshTokenProperties(Duration expiration) {
        this(
                expiration,
                Duration.ofDays(30),
                Duration.ofDays(1),
                "0 0 * * * *",
                100);
    }

    @AssertTrue(message = "Refresh token expiration must be positive")
    public boolean isExpirationPositive() {
        return expiration == null || expiration.compareTo(Duration.ZERO) > 0;
    }

    @AssertTrue(message = "Refresh token family lifetime must be positive")
    public boolean isFamilyLifetimePositive() {
        return familyLifetime == null || familyLifetime.compareTo(Duration.ZERO) > 0;
    }

    @AssertTrue(message = "Refresh token cleanup retention must be positive")
    public boolean isCleanupRetentionPositive() {
        return cleanupRetention == null || cleanupRetention.compareTo(Duration.ZERO) > 0;
    }
}
