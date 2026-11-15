package com.smarthospital.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarthospital.persistence.MySqlIntegrationTest;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties = {
        "smart-hospital.security.refresh-token.cleanup-retention=PT1S",
        "smart-hospital.security.refresh-token.cleanup-cron=*/1 * * * * *",
        "smart-hospital.security.refresh-token.cleanup-batch-size=100"
})
@Import(RefreshTokenCleanupTest.FixedClockConfiguration.class)
class RefreshTokenCleanupTest extends MySqlIntegrationTest {

    private static final Instant NOW = Instant.parse("2030-01-15T00:00:00Z");

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void scheduledCleanupDeletesOnlyExpiredFamiliesAndRetainsLiveFamilies() throws Exception {
        User user = persistUser();
        String expiredHash = "e".repeat(64);
        String liveHash = "l".repeat(64);
        refreshTokenRepository.saveAndFlush(new RefreshToken(
                user,
                expiredHash,
                "expired-family",
                NOW.minusSeconds(5),
                NOW.minusSeconds(10),
                user.getAuthVersion()));
        refreshTokenRepository.saveAndFlush(new RefreshToken(
                user,
                liveHash,
                "live-family",
                NOW.plusSeconds(60 * 60),
                NOW.plusSeconds(2 * 60 * 60),
                user.getAuthVersion()));

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline
                && refreshTokenRepository.findByTokenHash(expiredHash).isPresent()) {
            Thread.sleep(100);
        }

        assertThat(refreshTokenRepository.findByTokenHash(expiredHash)).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash(liveHash)).isPresent();
    }

    private User persistUser() {
        String unique = UUID.randomUUID().toString();
        return userRepository.saveAndFlush(new User(
                UserRole.PATIENT,
                "Cleanup Test Patient",
                "cleanup-" + unique + "@example.test",
                passwordEncoder.encode("StrongPassword@123"),
                "+1-555-0100"));
    }

    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock refreshTokenCleanupFixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
