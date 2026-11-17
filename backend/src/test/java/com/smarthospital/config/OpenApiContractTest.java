package com.smarthospital.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthospital.persistence.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void openApiContractContainsMetadataServerAndBearerScheme() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode document = objectMapper.readTree(result.getResponse().getContentAsByteArray());

        assertThat(document.path("info").path("title").asText())
                .isEqualTo("Smart Hospital API");
        assertThat(document.path("info").path("version").asText()).isEqualTo("1.0.0");
        assertThat(document.path("info").path("description").asText())
                .isEqualTo("REST API for Smart Hospital role-based workflows.");
        assertThat(document.path("servers").get(0).path("url").asText())
                .isEqualTo("http://localhost:8080");

        JsonNode bearer = document.path("components").path("securitySchemes").path("bearerAuth");
        assertThat(bearer.path("type").asText()).isEqualTo("http");
        assertThat(bearer.path("scheme").asText()).isEqualTo("bearer");
        assertThat(bearer.path("bearerFormat").asText()).isEqualTo("JWT");
    }

    @Test
    void dashboardOperationsAreTaggedAndProtectedByBearerAuthentication() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode paths = objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .path("paths");

        JsonNode patient = paths.path("/api/v1/dashboard/patient").path("get");
        assertThat(patient.path("tags").toString()).contains("Patient");
        assertThat(patient.path("security").get(0).path("bearerAuth").toString())
                .isEqualTo("[]");

        JsonNode doctor = paths.path("/api/v1/dashboard/doctor").path("get");
        assertThat(doctor.path("tags").toString()).contains("Doctor");
        assertThat(doctor.path("parameters").toString()).doesNotContain("doctorId");

        JsonNode admin = paths.path("/api/v1/dashboard/admin").path("get");
        assertThat(admin.path("tags").toString()).contains("Admin");
        assertThat(admin.path("parameters").toString()).contains("date");
    }

    @Test
    void publicAuthOperationIsTaggedPublicAndHasNoBearerRequirement() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode register = objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .path("paths")
                .path("/api/v1/auth/register")
                .path("post");

        assertThat(register.path("tags").toString()).contains("Public");
        assertThat(register.path("security").isMissingNode()).isTrue();
    }

    @Test
    void exactAnonymousRoutePolicyIsPreserved() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/api/v1/dashboard/patient"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/dashboard/doctor"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/dashboard/admin"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isUnauthorized());
    }
}
