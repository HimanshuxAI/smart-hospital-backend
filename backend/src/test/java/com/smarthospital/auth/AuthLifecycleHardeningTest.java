package com.smarthospital.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthospital.persistence.MySqlIntegrationTest;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class AuthLifecycleHardeningTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Test
    void deactivationImmediatelyRevokesAnAlreadyIssuedRefreshCredential() throws Exception {
        JsonNode bundle = register(uniqueEmail());
        Long userId = bundle.get("user").get("id").asLong();

        deactivate(userId);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + bundle.get("accessToken").asText()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(post("/api/v1/auth/refresh").with(uniqueRemoteAddress())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "refreshToken", bundle.get("refreshToken").asText()))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void reactivationDoesNotResurrectOldAccessOrRefreshCredentials() throws Exception {
        String email = uniqueEmail();
        JsonNode bundle = register(email);
        Long userId = bundle.get("user").get("id").asLong();

        deactivate(userId);
        reactivate(userId);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + bundle.get("accessToken").asText()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(post("/api/v1/auth/refresh").with(uniqueRemoteAddress())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "refreshToken", bundle.get("refreshToken").asText()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void deactivationRevokesTheRotatedSuccessorRefreshCredential() throws Exception {
        JsonNode original = register(uniqueEmail());
        Long userId = original.get("user").get("id").asLong();

        MvcResult rotation = mockMvc.perform(post("/api/v1/auth/refresh").with(uniqueRemoteAddress())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "refreshToken", original.get("refreshToken").asText()))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode successor = objectMapper.readTree(rotation.getResponse().getContentAsByteArray());

        deactivate(userId);

        mockMvc.perform(post("/api/v1/auth/refresh").with(uniqueRemoteAddress())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "refreshToken", successor.get("refreshToken").asText()))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void freshCredentialsWorkAfterReactivation() throws Exception {
        String email = uniqueEmail();
        String password = "StrongPassword@123";
        JsonNode bundle = register(email);
        Long userId = bundle.get("user").get("id").asLong();

        deactivate(userId);
        reactivate(userId);

        mockMvc.perform(post("/api/v1/auth/login").with(uniqueRemoteAddress())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "email", email,
                                "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    private JsonNode register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register").with(uniqueRemoteAddress())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "fullName", "Lifecycle Patient",
                                "email", email,
                                "password", "StrongPassword@123",
                                "phone", "+1-555-0100",
                                "dateOfBirth", "1990-01-15",
                                "gender", "Unspecified",
                                "address", "100 Test Street",
                                "emergencyContact", "Emergency Contact +1-555-0199"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private void deactivate(Long userId) throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/{id}/activation", userId)
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    private void reactivate(Long userId) throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/{id}/activation", userId)
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("active", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    private String adminToken() {
        User admin = userRepository.saveAndFlush(new User(
                UserRole.ADMIN,
                "Lifecycle Admin",
                uniqueEmail(),
                passwordEncoder.encode("StrongPassword@123"),
                "+1-555-0100"));
        return jwtService.issueAccessToken(admin);
    }

    private static RequestPostProcessor uniqueRemoteAddress() {
        return request -> {
            request.setRemoteAddr(UUID.randomUUID().toString());
            return request;
        };
    }

    private static String uniqueEmail() {
        return "lifecycle-" + UUID.randomUUID() + "@example.test";
    }
}
