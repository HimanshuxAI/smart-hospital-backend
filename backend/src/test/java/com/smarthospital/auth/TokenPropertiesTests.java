package com.smarthospital.auth;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
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
}
