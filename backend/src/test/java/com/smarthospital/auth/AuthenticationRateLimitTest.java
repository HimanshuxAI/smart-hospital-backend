package com.smarthospital.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {
        "smart-hospital.security.rate-limit.window=PT1H",
        "smart-hospital.security.rate-limit.max-tracked-entries=100",
        "smart-hospital.security.rate-limit.login.account-limit=2",
        "smart-hospital.security.rate-limit.login.ip-limit=2",
        "smart-hospital.security.rate-limit.registration.account-limit=1",
        "smart-hospital.security.rate-limit.registration.ip-limit=2",
        "smart-hospital.security.rate-limit.refresh.token-limit=2",
        "smart-hospital.security.rate-limit.refresh.ip-limit=2"
})
@AutoConfigureMockMvc
@Import(AuthenticationRateLimitTest.MutableClockConfiguration.class)
class AuthenticationRateLimitTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MutableClock clock;

    @Test
    void loginAccountLimitIsIndependentFromTheIpLimit() throws Exception {
        String email = uniqueEmail();
        String firstIp = uniqueIp();
        String secondIp = uniqueIp();
        String thirdIp = uniqueIp();

        login(email, firstIp).andExpect(status().isUnauthorized());
        login(email, secondIp).andExpect(status().isUnauthorized());
        login(email, thirdIp)
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(header().string("Retry-After", org.hamcrest.Matchers.matchesPattern("[1-9][0-9]*")));
    }

    @Test
    void loginIpLimitIsIndependentFromTheAccountLimit() throws Exception {
        String ip = uniqueIp();

        login(uniqueEmail(), ip).andExpect(status().isUnauthorized());
        login(uniqueEmail(), ip).andExpect(status().isUnauthorized());
        login(uniqueEmail(), ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void registrationAccountLimitIsIndependentFromTheIpLimit() throws Exception {
        String email = uniqueEmail();
        String ip = uniqueIp();

        register(email, ip).andExpect(status().isOk());
        register(email, ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void registrationIpLimitIsIndependentFromTheAccountLimit() throws Exception {
        String ip = uniqueIp();

        register(uniqueEmail(), ip).andExpect(status().isOk());
        register(uniqueEmail(), ip).andExpect(status().isOk());
        register(uniqueEmail(), ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void refreshTokenLimitIsIndependentFromTheIpLimit() throws Exception {
        String token = "t".repeat(43);
        String firstIp = uniqueIp();
        String secondIp = uniqueIp();
        String thirdIp = uniqueIp();

        refresh(token, firstIp).andExpect(status().isUnauthorized());
        refresh(token, secondIp).andExpect(status().isUnauthorized());
        refresh(token, thirdIp)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void refreshIpLimitIsIndependentFromTheTokenLimit() throws Exception {
        String ip = uniqueIp();

        refresh("a".repeat(43), ip).andExpect(status().isUnauthorized());
        refresh("b".repeat(43), ip).andExpect(status().isUnauthorized());
        refresh("c".repeat(43), ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void aNewFixedWindowAllowsAttemptsAgain() throws Exception {
        String email = uniqueEmail();

        login(email, uniqueIp()).andExpect(status().isUnauthorized());
        login(email, uniqueIp()).andExpect(status().isUnauthorized());
        login(email, uniqueIp()).andExpect(status().isTooManyRequests());

        clock.advance(Duration.ofHours(1).plusSeconds(1));

        login(email, uniqueIp()).andExpect(status().isUnauthorized());
    }

    private ResultActions login(String email, String remoteAddress) throws Exception {
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

    private ResultActions register(String email, String remoteAddress) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                        .with(request -> {
                            request.setRemoteAddr(remoteAddress);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "fullName", "Rate Limit Patient",
                                "email", email,
                                "password", "StrongPassword@123",
                                "phone", "+1-555-0100",
                                "dateOfBirth", "1990-01-15",
                                "gender", "Unspecified",
                                "address", "100 Test Street",
                                "emergencyContact", "Emergency Contact +1-555-0199"))));
    }

    private ResultActions refresh(String token, String remoteAddress) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(request -> {
                            request.setRemoteAddr(remoteAddress);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("refreshToken", token))));
    }

    private static String uniqueEmail() {
        return "rate-limit-" + UUID.randomUUID() + "@example.test";
    }

    private static final java.util.concurrent.atomic.AtomicInteger IP_COUNTER = new java.util.concurrent.atomic.AtomicInteger(1);

    private static String uniqueIp() {
        int val = IP_COUNTER.getAndIncrement();
        return "10." + ((val >> 16) & 0xff) + "." + ((val >> 8) & 0xff) + "." + (val & 0xff);
    }

    static class MutableClockConfiguration {

        @Bean
        @Primary
        MutableClock authenticationRateLimitMutableClock() {
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
