package com.smarthospital.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthospital.persistence.MySqlIntegrationTest;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registrationReturnsExactTokenBundleShapeWithoutSensitiveFields() throws Exception {
        String email = uniqueEmail();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register").with(uniqueRemoteAddress())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(registration(email))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.id").isNumber())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("PATIENT"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        JsonNode bundle = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertThat(bundle.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("accessToken", "refreshToken", "tokenType", "expiresIn", "user");
    }

    @Test
    void registrationRejectsRoleAndDoctorFieldsWithoutCreatingPrivilegedUser() throws Exception {
        String email = uniqueEmail();
        Map<String, Object> request = new LinkedHashMap<>(registration(email));
        request.put("role", "DOCTOR");
        request.put("licenseNumber", "UNTRUSTED-LICENSE");

        mockMvc.perform(post("/api/v1/auth/register").with(uniqueRemoteAddress())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").isNotEmpty());

        assertThat(userRepository.findByEmailIgnoreCase(email)).isEmpty();
    }

    @Test
    void duplicateRegistrationReturnsStableProblemDetails() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/api/v1/auth/register").with(uniqueRemoteAddress())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(registration(email.toUpperCase()))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Email is already registered"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.type").isNotEmpty())
                .andExpect(jsonPath("$.instance").isNotEmpty());
    }

    @Test
    void validationFailureIncludesFieldErrorsAndProblemMetadata() throws Exception {
        Map<String, Object> request = new LinkedHashMap<>(registration(uniqueEmail()));
        request.put("email", "not-an-email");

        mockMvc.perform(post("/api/v1/auth/register").with(uniqueRemoteAddress())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.email").isNotEmpty());
    }

    @Test
    void malformedJsonReturnsProblemDetails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").with(uniqueRemoteAddress())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void unknownEmailAndWrongPasswordReturnTheSameNonEnumeratingProblem() throws Exception {
        String email = uniqueEmail();
        register(email);

        MvcResult wrongPassword = mockMvc.perform(post("/api/v1/auth/login").with(uniqueRemoteAddress())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "email", email,
                                "password", "WrongPassword@123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn();
        MvcResult unknownEmail = mockMvc.perform(post("/api/v1/auth/login").with(uniqueRemoteAddress())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "email", uniqueEmail(),
                                "password", "StrongPassword@123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn();

        JsonNode wrongProblem = objectMapper.readTree(wrongPassword.getResponse().getContentAsByteArray());
        JsonNode unknownProblem = objectMapper.readTree(unknownEmail.getResponse().getContentAsByteArray());
        assertThat(wrongProblem.get("code")).isEqualTo(unknownProblem.get("code"));
        assertThat(wrongProblem.get("message")).isEqualTo(unknownProblem.get("message"));
        assertThat(wrongProblem.get("detail")).isEqualTo(unknownProblem.get("detail"));
    }

    @Test
    void refreshRotatesTokenAndReturnsANewBundle() throws Exception {
        JsonNode original = register(uniqueEmail());

        mockMvc.perform(post("/api/v1/auth/refresh").with(uniqueRemoteAddress())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "refreshToken", original.get("refreshToken").asText()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.role").value("PATIENT"));
    }

    @Test
    void logoutRequiresAuthenticationAndRevokesThePresentedFamily() throws Exception {
        JsonNode bundle = register(uniqueEmail());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "refreshToken", bundle.get("refreshToken").asText()))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + bundle.get("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "refreshToken", bundle.get("refreshToken").asText()))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh").with(uniqueRemoteAddress())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "refreshToken", bundle.get("refreshToken").asText()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logoutCannotRevokeAnotherUsersRefreshTokenFamily() throws Exception {
        JsonNode firstUser = register(uniqueEmail());
        JsonNode secondUser = register(uniqueEmail());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + firstUser.get("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "refreshToken", secondUser.get("refreshToken").asText()))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        mockMvc.perform(post("/api/v1/auth/refresh").with(uniqueRemoteAddress())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "refreshToken", secondUser.get("refreshToken").asText()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void meReturnsAuthenticatedIdentityAndProfileSummary() throws Exception {
        JsonNode bundle = register(uniqueEmail());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + bundle.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bundle.get("user").get("id").asLong()))
                .andExpect(jsonPath("$.email").value(bundle.get("user").get("email").asText()))
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.patientProfile.dateOfBirth").value("1990-01-15"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void unsupportedRequestMediaTypeReturnsProblemDetails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").with(uniqueRemoteAddress())
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<login/>"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void unsupportedResponseMediaTypeReturnsProblemDetails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").with(uniqueRemoteAddress())
                        .accept(MediaType.APPLICATION_XML)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(registration(uniqueEmail()))))
                .andExpect(status().isNotAcceptable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_ACCEPTABLE"));
    }

    @Test
    void actuatorHealthRootIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void actuatorHealthSubpathsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/actuator/health/liveness"));

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/actuator/health/readiness"));
    }

    @Test
    void openApiDocumentsArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void swaggerUiEntryPointIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void missingAndInvalidBearerTokensUseProblemJson() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/me"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/me"));
    }

    @Test
    void deactivatedUserCannotUseAlreadyIssuedAccessToken() throws Exception {
        JsonNode bundle = register(uniqueEmail());
        User user = userRepository.findByEmailIgnoreCase(bundle.get("user").get("email").asText()).orElseThrow();
        user.setActive(false);
        userRepository.saveAndFlush(user);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + bundle.get("accessToken").asText()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void inactiveUserCannotLoginOrRefresh() throws Exception {
        String email = uniqueEmail();
        JsonNode bundle = register(email);
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setActive(false);
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/v1/auth/login").with(uniqueRemoteAddress())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "email", email,
                                "password", "StrongPassword@123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_INACTIVE"));

        mockMvc.perform(post("/api/v1/auth/refresh").with(uniqueRemoteAddress())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "refreshToken", bundle.get("refreshToken").asText()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_INACTIVE"));
    }

    private JsonNode register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register").with(uniqueRemoteAddress())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(registration(email))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private static Map<String, Object> registration(String email) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("fullName", "HTTP Test Patient");
        request.put("email", email);
        request.put("password", "StrongPassword@123");
        request.put("phone", "+1-555-0100");
        request.put("dateOfBirth", "1990-01-15");
        request.put("gender", "Unspecified");
        request.put("address", "100 Test Street");
        request.put("emergencyContact", "Emergency Contact +1-555-0199");
        return request;
    }

    private static RequestPostProcessor uniqueRemoteAddress() {
        return request -> {
            request.setRemoteAddr(UUID.randomUUID().toString());
            return request;
        };
    }

    private static String uniqueEmail() {
        return "http-" + UUID.randomUUID() + "@example.test";
    }
}
