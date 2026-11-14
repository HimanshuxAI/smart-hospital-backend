package com.smarthospital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

class ApplicationProfileConfigurationTests {

    @Test
    void commonConfigurationRequiresDatabaseEnvironmentValues() throws IOException {
        PropertySource<?> configuration = load("application.yml");

        assertThat(value(configuration, "spring.datasource.url")).isEqualTo("${DB_URL}");
        assertThat(value(configuration, "spring.datasource.username")).isEqualTo("${DB_USERNAME}");
        assertThat(value(configuration, "spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
        assertThat(value(configuration, "spring.datasource.url"))
                .doesNotContain("useSSL=false", "allowPublicKeyRetrieval=true");
    }

    @Test
    void commonConfigurationRequiresSecurityEnvironmentValues() throws IOException {
        PropertySource<?> configuration = load("application.yml");

        assertThat(value(configuration, "spring.security.user.name")).isEqualTo("${SECURITY_USERNAME}");
        assertThat(value(configuration, "spring.security.user.password")).isEqualTo("${SECURITY_PASSWORD}");
        assertThat(value(configuration, "smart-hospital.security.jwt.secret")).isEqualTo("${JWT_SECRET}");
    }

    @Test
    void commonConfigurationSeparatesAccessAndRefreshTokenLifetimes() throws IOException {
        PropertySource<?> configuration = load("application.yml");

        assertThat(value(configuration, "smart-hospital.security.jwt.access-token-expiration"))
                .isEqualTo("${JWT_ACCESS_TOKEN_EXPIRATION:PT15M}");
        assertThat(value(configuration, "smart-hospital.security.refresh-token.expiration"))
                .isEqualTo("${JWT_REFRESH_TOKEN_EXPIRATION:P7D}");
        assertThat(value(configuration, "smart-hospital.security.jwt.refresh-token-expiration"))
                .isNull();
    }

    @Test
    void localConfigurationDefinesExplicitLocalDatabaseDefaults() throws IOException {
        PropertySource<?> configuration = load("application-local.yml");

        assertThat(value(configuration, "spring.datasource.url"))
                .isEqualTo("jdbc:mysql://localhost:3306/smart_hospital_local?serverTimezone=UTC");
        assertThat(value(configuration, "spring.datasource.username"))
                .isEqualTo("smart_hospital_local");
        assertThat(value(configuration, "spring.datasource.password"))
                .isEqualTo("local-development-password");
        assertThat(value(configuration, "spring.datasource.url"))
                .doesNotContain("useSSL=false", "allowPublicKeyRetrieval=true");
        assertThat(value(configuration, "spring.datasource.hikari.data-source-properties.sslMode"))
                .isNull();
        assertThat(value(configuration, "spring.datasource.hikari.data-source-properties.useSSL"))
                .isNull();
        assertThat(value(configuration, "spring.security.user.name")).isEqualTo("local-user");
        assertThat(value(configuration, "spring.security.user.password"))
                .isEqualTo("local-development-password");
        assertThat(value(configuration, "smart-hospital.security.jwt.secret"))
                .isEqualTo("local-development-jwt-secret-change-before-production");
    }

    @Test
    void productionConfigurationRequiresEnvironmentValuesAndVerifiesTls() throws IOException {
        PropertySource<?> configuration = load("application-prod.yml");

        assertThat(value(configuration, "spring.datasource.url")).isEqualTo("${DB_URL}");
        assertThat(value(configuration, "spring.datasource.username")).isEqualTo("${DB_USERNAME}");
        assertThat(value(configuration, "spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
        assertThat(value(configuration, "spring.datasource.hikari.data-source-properties.sslMode"))
                .isEqualTo("VERIFY_IDENTITY");
        assertThat(value(configuration, "spring.datasource.url"))
                .doesNotContain("useSSL=false", "allowPublicKeyRetrieval=true");
        assertThat(value(configuration, "spring.security.user.name")).isEqualTo("${SECURITY_USERNAME}");
        assertThat(value(configuration, "spring.security.user.password")).isEqualTo("${SECURITY_PASSWORD}");
        assertThat(value(configuration, "smart-hospital.security.jwt.secret")).isEqualTo("${JWT_SECRET}");
    }

    @Test
    void localProfileResolvesLocalDefaults() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=local")
                .run(context -> {
                    assertThat(context.getEnvironment().getProperty("spring.datasource.url"))
                            .isEqualTo("jdbc:mysql://localhost:3306/smart_hospital_local?serverTimezone=UTC");
                    assertThat(context.getEnvironment().getProperty("spring.datasource.username"))
                            .isEqualTo("smart_hospital_local");
                    assertThat(context.getEnvironment().getProperty("spring.datasource.password"))
                            .isEqualTo("local-development-password");
                    assertThat(context.getEnvironment().getProperty("spring.security.user.name"))
                            .isEqualTo("local-user");
                    assertThat(context.getEnvironment().getProperty("smart-hospital.security.jwt.secret"))
                            .isEqualTo("local-development-jwt-secret-change-before-production");
                });
    }

    @Test
    void productionProfileResolvesEnvironmentValuesAndTlsConfiguration() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "DB_URL=jdbc:mysql://prod.example/smart_hospital",
                        "DB_USERNAME=prod-user",
                        "DB_PASSWORD=prod-password",
                        "SECURITY_USERNAME=prod-security-user",
                        "SECURITY_PASSWORD=prod-security-password",
                        "JWT_SECRET=production-test-jwt-secret")
                .run(context -> {
                    assertThat(context.getEnvironment().getProperty("spring.datasource.url"))
                            .isEqualTo("jdbc:mysql://prod.example/smart_hospital");
                    assertThat(context.getEnvironment().getProperty("spring.datasource.username"))
                            .isEqualTo("prod-user");
                    assertThat(context.getEnvironment().getProperty("spring.datasource.password"))
                            .isEqualTo("prod-password");
                    assertThat(context.getEnvironment().getProperty("spring.security.user.name"))
                            .isEqualTo("prod-security-user");
                    assertThat(context.getEnvironment().getProperty("smart-hospital.security.jwt.secret"))
                            .isEqualTo("production-test-jwt-secret");
                    assertThat(context.getEnvironment()
                                    .getProperty("spring.datasource.hikari.data-source-properties.sslMode"))
                            .isEqualTo("VERIFY_IDENTITY");
                });
    }

    @Test
    void productionProfileFailsFastWhenRequiredEnvironmentValuesAreMissing() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("spring.profiles.active", "prod");

        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.setEnvironment(environment);
            new ConfigDataApplicationContextInitializer().initialize(context);

            assertThatThrownBy(() -> environment.getProperty("spring.datasource.url"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Could not resolve placeholder 'DB_URL'");
            assertThatThrownBy(() -> environment.getProperty("spring.security.user.name"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Could not resolve placeholder 'SECURITY_USERNAME'");
            assertThatThrownBy(() -> environment.getProperty("smart-hospital.security.jwt.secret"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Could not resolve placeholder 'JWT_SECRET'");
        }
    }

    private static String value(PropertySource<?> configuration, String propertyName) {
        Object propertyValue = configuration.getProperty(propertyName);
        return propertyValue == null ? null : propertyValue.toString();
    }

    private static PropertySource<?> load(String resourceName) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourceName);
        assertThat(resource.exists()).as("classpath resource %s", resourceName).isTrue();

        List<PropertySource<?>> propertySources = new YamlPropertySourceLoader().load(resourceName, resource);
        assertThat(propertySources).hasSize(1);
        return propertySources.getFirst();
    }
}
