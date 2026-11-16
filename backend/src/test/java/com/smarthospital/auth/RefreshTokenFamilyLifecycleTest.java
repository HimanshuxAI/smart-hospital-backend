package com.smarthospital.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smarthospital.common.ApiException;
import com.smarthospital.persistence.MySqlIntegrationTest;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties = {
        "smart-hospital.security.refresh-token.family-lifetime=PT720H"
})
@Import(RefreshTokenFamilyLifecycleTest.FixedClockConfiguration.class)
class RefreshTokenFamilyLifecycleTest extends MySqlIntegrationTest {

    private static final Instant NOW = Instant.parse("2030-01-15T00:00:00Z");

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenProperties refreshTokenProperties;

    @Test
    void successorInheritsTheImmutableFamilyDeadlineAndNeverExpiresBeyondIt() {
        User user = persistUser();

        assertThat(refreshTokenProperties.familyLifetime()).isEqualTo(java.time.Duration.ofHours(720));
        RefreshTokenService.IssuedRefreshToken original = refreshTokenService.issue(user);
        RefreshToken originalRow = refreshTokenRepository
                .findByTokenHash(original.tokenHash())
                .orElseThrow();
        assertThat(originalRow.getFamilyDeadline()).isEqualTo(NOW.plusSeconds(720 * 60 * 60));
        assertThat(originalRow.getExpiresAt()).isEqualTo(NOW.plusSeconds(7 * 24 * 60 * 60));

        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(original.rawToken());
        RefreshToken successor = refreshTokenRepository
                .findByTokenHash(rotation.tokenHash())
                .orElseThrow();

        assertThat(successor.getFamilyDeadline()).isEqualTo(originalRow.getFamilyDeadline());
        assertThat(successor.getExpiresAt()).isEqualTo(NOW.plusSeconds(7 * 24 * 60 * 60));
        assertThat(successor.getExpiresAt()).isBeforeOrEqualTo(successor.getFamilyDeadline());
    }

    @Test
    void refreshRowStoresTheUsersAuthenticationVersion() {
        User user = persistUser();

        RefreshTokenService.IssuedRefreshToken issued = refreshTokenService.issue(user);

        RefreshToken row = refreshTokenRepository.findByTokenHash(issued.tokenHash()).orElseThrow();
        assertThat(row.getAuthVersion()).isEqualTo(user.getAuthVersion());
    }

    @Test
    void refreshRowWithMismatchedAuthenticationVersionRevokesItsFamily() {
        User user = persistUser();
        String rawToken = RefreshTokenService.generateRawToken();
        String tokenHash = RefreshTokenService.hash(rawToken);
        String familyId = UUID.randomUUID().toString();
        refreshTokenRepository.saveAndFlush(new RefreshToken(
                user,
                tokenHash,
                familyId,
                NOW.plusSeconds(60 * 60),
                NOW.plusSeconds(2 * 60 * 60),
                user.getAuthVersion() + 1));

        assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_REFRESH_TOKEN"));
        assertThat(refreshTokenRepository.findByFamilyId(familyId))
                .allSatisfy(token -> assertThat(token.getRevokedAt()).isNotNull());
    }

    @Test
    void expiredFamilyDeadlineRejectsAndRevokesTheFamily() {
        User user = persistUser();
        String rawToken = RefreshTokenService.generateRawToken();
        String tokenHash = RefreshTokenService.hash(rawToken);
        String familyId = UUID.randomUUID().toString();
        refreshTokenRepository.saveAndFlush(new RefreshToken(
                user,
                tokenHash,
                familyId,
                NOW.plusSeconds(60 * 60),
                NOW.minusSeconds(1),
                user.getAuthVersion()));

        assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_REFRESH_TOKEN"));
        assertThat(refreshTokenRepository.findByFamilyId(familyId))
                .allSatisfy(token -> assertThat(token.getRevokedAt()).isNotNull());
    }

    private User persistUser() {
        String unique = UUID.randomUUID().toString();
        return userRepository.saveAndFlush(new User(
                UserRole.PATIENT,
                "Family Test Patient",
                "family-" + unique + "@example.test",
                passwordEncoder.encode("StrongPassword@123"),
                "+1-555-0100"));
    }

    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock refreshTokenFamilyFixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
