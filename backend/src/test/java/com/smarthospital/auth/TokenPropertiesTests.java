package com.smarthospital.auth;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class TokenPropertiesTests {

    private static final String VALID_SECRET = "test-jwt-secret-with-at-least-32-characters";

    @Test
    void stringRepresentationDoesNotExposeSecret() {
        TokenProperties tokenProperties = new TokenProperties(VALID_SECRET, Duration.ofMinutes(15));

        assertThat(tokenProperties.toString())
                .doesNotContain(VALID_SECRET)
                .isEqualTo("TokenProperties[secret=REDACTED, accessTokenExpiration=PT15M]");
    }

    @Test
    void missingAccessTokenExpirationReportsOnlyConfigurationError() {
        TokenProperties tokenProperties = new TokenProperties(VALID_SECRET, null);

        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = validatorFactory.getValidator();

            assertThat(validator.validate(tokenProperties))
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactly("Access token expiration must be configured");
        }
    }

    @Test
    void issuerMustNotBeBlank() {
        TokenProperties tokenProperties = new TokenProperties(
                VALID_SECRET,
                Duration.ofMinutes(15),
                "   ");

        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = validatorFactory.getValidator();

            assertThat(validator.validate(tokenProperties))
                    .extracting(ConstraintViolation::getMessage)
                    .contains("JWT issuer must not be blank");
        }
    }

    @Test
    void unresolvedIssuerPlaceholderIsRejected() {
        TokenProperties tokenProperties = new TokenProperties(
                VALID_SECRET,
                Duration.ofMinutes(15),
                "${JWT_ISSUER}");

        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = validatorFactory.getValidator();

            assertThat(validator.validate(tokenProperties))
                    .extracting(ConstraintViolation::getMessage)
                    .contains("JWT issuer must be configured");
        }
    }

    @Test
    void refreshTokenExpirationMustBePositive() {
        RefreshTokenProperties tokenProperties = new RefreshTokenProperties(Duration.ZERO);

        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = validatorFactory.getValidator();

            assertThat(validator.validate(tokenProperties))
                    .extracting(ConstraintViolation::getMessage)
                    .contains("Refresh token expiration must be positive");
        }
    }

    @Test
    void secretWithAtLeastThirtyTwoCharactersHasAtLeastThirtyTwoUtf8Bytes() {
        String validSecret = "a".repeat(32);
        TokenProperties tokenProperties = new TokenProperties(
                validSecret,
                Duration.ofMinutes(15),
                "smart-hospital-test");

        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = validatorFactory.getValidator();

            assertThat(validSecret.getBytes(StandardCharsets.UTF_8)).hasSizeGreaterThanOrEqualTo(32);
            assertThat(validator.validate(tokenProperties)).isEmpty();
        }
    }
}
