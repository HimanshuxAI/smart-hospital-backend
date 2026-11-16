package com.smarthospital.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarthospital.SmartHospitalApplication;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.StandardEnvironment;

class JwtSecretStartupValidationTests {

    private static final String VALID_SECRET = "test-jwt-secret-with-at-least-32-characters";

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withInitializer(hermeticConfigDataInitializer())
            .withUserConfiguration(SmartHospitalApplication.class)
            .withPropertyValues(
                    "spring.profiles.active=prod",
                    "DB_URL=jdbc:h2:mem:jwt-secret-startup;MODE=MySQL;DB_CLOSE_DELAY=-1",
                    "DB_USERNAME=sa",
                    "DB_PASSWORD=",
                    "SECURITY_USERNAME=test-user",
                    "SECURITY_PASSWORD=test-password",
                    "JWT_ISSUER=smart-hospital-backend-test",
                    "JWT_REFRESH_TOKEN_EXPIRATION=P7D",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.flyway.enabled=false");

    private final WebApplicationContextRunner localContextRunner = new WebApplicationContextRunner()
            .withInitializer(hermeticConfigDataInitializer())
            .withUserConfiguration(SmartHospitalApplication.class)
            .withPropertyValues(
                    "spring.profiles.active=local",
                    "spring.datasource.url=jdbc:h2:mem:jwt-secret-local;MODE=MySQL;DB_CLOSE_DELAY=-1",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.flyway.enabled=false");

    @Test
    void productionStartupFailsWhenJwtSecretIsMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("TokenProperties")
                    .hasStackTraceContaining("JWT secret must be configured")
                    .hasStackTraceContaining("JWT_SECRET");
        });
    }

    @Test
    void productionStartupRejectsUnresolvedJwtSecretPlaceholderRegardlessOfLength() {
        String unresolvedPlaceholder = "${JWT_SECRET_WITH_A_NAME_THAT_IS_LONG_ENOUGH_TO_PASS_SIZE}";

        contextRunner.withPropertyValues("JWT_SECRET=" + unresolvedPlaceholder).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("JWT secret must be configured");
        });
    }

    @Test
    void productionStartupRejectsBlankJwtSecret() {
        contextRunner.withPropertyValues("JWT_SECRET=   ").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("JWT secret must not be blank");
        });
    }

    @Test
    void productionStartupRejectsShortJwtSecret() {
        contextRunner.withPropertyValues("JWT_SECRET=too-short").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("JWT secret must contain at least 32 characters");
        });
    }

    @Test
    void productionStartupRejectsZeroAccessTokenExpiration() {
        contextRunner.withPropertyValues(
                "JWT_SECRET=" + VALID_SECRET,
                "JWT_ACCESS_TOKEN_EXPIRATION=PT0S").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("Access token expiration must be positive");
        });
    }

    @Test
    void productionStartupRejectsNegativeAccessTokenExpiration() {
        contextRunner.withPropertyValues(
                "JWT_SECRET=" + VALID_SECRET,
                "JWT_ACCESS_TOKEN_EXPIRATION=PT-1S").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("Access token expiration must be positive");
        });
    }

    @Test
    void productionStartupRejectsBlankJwtIssuer() {
        contextRunner.withPropertyValues("JWT_ISSUER=   ").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("JWT issuer must not be blank");
        });
    }

    @Test
    void productionStartupRejectsNonPositiveRefreshTokenExpiration() {
        contextRunner.withPropertyValues(
                "JWT_SECRET=" + VALID_SECRET,
                "JWT_REFRESH_TOKEN_EXPIRATION=PT0S").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("Refresh token expiration must be positive");
        });
    }

    @Test
    void productionStartupBindsValidJwtSecret() {
        contextRunner.withPropertyValues(
                "JWT_SECRET=" + VALID_SECRET,
                "JWT_ACCESS_TOKEN_EXPIRATION=PT15M").run(context -> {
            assertThat(context).hasNotFailed();

            TokenProperties tokenProperties = context.getBean(TokenProperties.class);
            assertThat(tokenProperties.secret()).isEqualTo(VALID_SECRET);
            assertThat(tokenProperties.accessTokenExpiration()).isEqualTo(Duration.ofMinutes(15));
            assertThat(tokenProperties.issuer()).isEqualTo("smart-hospital-backend-test");
        });
    }

    @Test
    void localProfileStartsWithExplicitDevelopmentSecret() {
        localContextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            TokenProperties tokenProperties = context.getBean(TokenProperties.class);
            assertThat(tokenProperties.secret())
                    .isEqualTo("local-development-jwt-secret-change-before-production");
            assertThat(tokenProperties.issuer()).isEqualTo("smart-hospital-backend-local");
        });
    }

    private static ApplicationContextInitializer<ConfigurableApplicationContext>
            hermeticConfigDataInitializer() {
        return context -> {
            context.getEnvironment()
                    .getPropertySources()
                    .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
            context.getEnvironment()
                    .getPropertySources()
                    .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
            new ConfigDataApplicationContextInitializer().initialize(context);
        };
    }
}
