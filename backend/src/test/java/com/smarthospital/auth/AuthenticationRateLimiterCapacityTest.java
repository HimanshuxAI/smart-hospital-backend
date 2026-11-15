package com.smarthospital.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthospital.persistence.MySqlIntegrationTest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "smart-hospital.security.rate-limit.window=PT1H",
        "smart-hospital.security.rate-limit.max-tracked-entries=2"
})
@AutoConfigureMockMvc
@Import(AuthenticationRateLimiterCapacityTest.MutableClockConfiguration.class)
class AuthenticationRateLimiterCapacityTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MutableClock clock;

    @Test
    void limiterFailsClosedAtCapacityAndPrunesExpiredWindows() throws Exception {
        login(uniqueEmail(), "198.51.100.10")
                .andExpect(status().isUnauthorized());
        login(uniqueEmail(), "198.51.100.11")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(header().exists("Retry-After"));

        clock.advance(Duration.ofHours(1).plusSeconds(1));

        login(uniqueEmail(), "198.51.100.12")
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String remoteAddress)
            throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .with(request -> {
                    request.setRemoteAddr(remoteAddress);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(Map.of(
                        "email", email,
                        "password", "WrongPassword@123"))));
    }

    private static String uniqueEmail() {
        return "capacity-" + UUID.randomUUID() + "@example.test";
    }

    static class MutableClockConfiguration {

        @Bean
        @Primary
        MutableClock authenticationRateLimiterCapacityMutableClock() {
            return new MutableClock(Instant.parse("2030-01-15T00:00:00Z"));
        }
    }

    static class MutableClock extends Clock {

        private final AtomicReference<Instant> instant;

        MutableClock(Instant instant) {
            this.instant = new AtomicReference<>(instant);
        }

        void advance(Duration duration) {
            instant.updateAndGet(value -> value.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
