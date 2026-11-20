package com.smarthospital.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("smart-hospital.security.cors")
@Validated
public record CorsProperties(
        @NotNull(message = "CORS allowed origins must be configured")
        List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = sanitize(allowedOrigins);
    }

    @AssertTrue(message = "CORS allowed origins must be exact origins")
    public boolean hasOnlyExactOrigins() {
        return allowedOrigins == null || allowedOrigins.stream().noneMatch(origin -> origin.contains("*"));
    }

    private static List<String> sanitize(List<String> origins) {
        if (origins == null) {
            return null;
        }
        return origins.stream()
                .filter(origin -> origin != null)
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .distinct()
                .toList();
    }
}
