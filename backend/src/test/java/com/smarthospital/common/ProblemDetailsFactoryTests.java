package com.smarthospital.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ProblemDetailsFactoryTests {

    @Test
    void problemTypeUsesLocaleIndependentLowercaseCode() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            ProblemDetailsFactory factory = new ProblemDetailsFactory(
                    Clock.fixed(Instant.parse("2026-11-15T00:00:00Z"), ZoneOffset.UTC));

            var problem = factory.create(
                    HttpStatus.CONFLICT,
                    "EMAIL_ALREADY_EXISTS",
                    "Email is already registered",
                    "/api/v1/auth/register");

            assertThat(problem.getType())
                    .hasToString("urn:smart-hospital:problem:email_already_exists");
        } finally {
            Locale.setDefault(originalLocale);
        }
    }
}
