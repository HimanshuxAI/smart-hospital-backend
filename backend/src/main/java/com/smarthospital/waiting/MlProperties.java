package com.smarthospital.waiting;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smart-hospital.ml")
public record MlProperties(
        URI baseUrl,
        Duration connectTimeout,
        Duration readTimeout) {

    public MlProperties {
        if (baseUrl == null) {
            baseUrl = URI.create("http://localhost:8000");
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofMillis(750);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofMillis(750);
        }
    }
}
