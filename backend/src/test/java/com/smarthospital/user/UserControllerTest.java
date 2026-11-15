package com.smarthospital.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthospital.auth.AuthService;
import com.smarthospital.auth.JwtService;
import com.smarthospital.auth.RegisterRequest;
import com.smarthospital.auth.TokenBundleResponse;
import com.smarthospital.department.Department;
import com.smarthospital.department.DepartmentRepository;
import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.doctor.DoctorProfileRepository;
import com.smarthospital.persistence.MySqlIntegrationTest;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest extends MySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void onlyAdminCanCreateDoctorAndCreationIsAtomic() throws Exception {
        String adminToken = createAdminToken();
        String patientToken = createPatientToken();
        String doctorToken = createDoctorToken().accessToken();
        Department department = createDepartment();
        String email = uniqueEmail();
        Map<String, Object> request = doctorRequest(email, uniqueLicense(), department.getId());

        mockMvc.perform(post("/api/v1/admin/doctors")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/v1/admin/doctors"));

        mockMvc.perform(post("/api/v1/admin/doctors")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(post("/api/v1/admin/doctors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("DOCTOR"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.departmentId").value(department.getId()))
                .andExpect(jsonPath("$.specialty").value("Cardiology"))
                .andExpect(jsonPath("$.averageConsultationMinutes").value(20))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        User created = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(created.getRole()).isEqualTo(UserRole.DOCTOR);
        assertThat(created.getPasswordHash()).isNotEqualTo(request.get("password"));
        assertThat(passwordEncoder.matches((String) request.get("password"), created.getPasswordHash())).isTrue();
        assertThat(doctorProfileRepository.findByUserId(created.getId())).isPresent();
    }

    @Test
    void duplicateDoctorEmailAndLicenseHaveStableConflictCodes() throws Exception {
        String adminToken = createAdminToken();
        Department department = createDepartment();
        String email = uniqueEmail();
        String license = uniqueLicense();
        createDoctor(adminToken, email, license, department.getId());

        mockMvc.perform(post("/api/v1/admin/doctors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                doctorRequest(email, uniqueLicense(), department.getId()))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));

        mockMvc.perform(post("/api/v1/admin/doctors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                doctorRequest(uniqueEmail(), license, department.getId()))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("DOCTOR_LICENSE_ALREADY_EXISTS"));
    }

    @Test
    void doctorCreationWithMissingDepartmentDoesNotLeavePartialUser() throws Exception {
        String adminToken = createAdminToken();
        String email = uniqueEmail();

        mockMvc.perform(post("/api/v1/admin/doctors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                doctorRequest(email, uniqueLicense(), 999999L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_NOT_FOUND"));

        assertThat(userRepository.findByEmailIgnoreCase(email)).isEmpty();
    }

    @Test
    void adminUserListNeverExposesPasswordHashes() throws Exception {
        String adminToken = createAdminToken();
        createPatientToken();

        MvcResult result = mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].email").isNotEmpty())
                .andExpect(jsonPath("$[0].role").isNotEmpty())
                .andExpect(jsonPath("$[0].active").isBoolean())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain("passwordHash").doesNotContain("$2a$").doesNotContain("$2b$");
    }

    @Test
    void adminCanDeactivateAndReactivateAccountImmediately() throws Exception {
        String adminToken = createAdminToken();
        String email = uniqueEmail();
        String password = "StrongPassword@123";
        TokenBundleResponse bundle = authService.register(new RegisterRequest(
                "Activation Patient",
                email,
                password,
                "+1-555-0100",
                LocalDate.of(1990, 1, 15),
                null,
                null,
                null));
        long userId = bundle.user().id();

        mockMvc.perform(patch("/api/v1/admin/users/{id}/activation", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + bundle.accessToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "refreshToken", bundle.refreshToken()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_INACTIVE"));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "email", email,
                                "password", password))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/admin/users/{id}/activation", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("active", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "email", email,
                                "password", password))))
                .andExpect(status().isOk());
    }

    @Test
    void invalidPathVariableReturnsValidationProblemDetails() throws Exception {
        String adminToken = createAdminToken();

        mockMvc.perform(patch("/api/v1/admin/users/not-a-number/activation")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("active", false))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.id").isNotEmpty());
    }

    @Test
    void doctorCanReadAndUpdateOnlyOwnProfileWhileAdminCanManageAnyProfile() throws Exception {
        String adminToken = createAdminToken();
        DoctorAccount doctor = createDoctorToken();
        DoctorAccount otherDoctor = createDoctorToken();
        String patientToken = createPatientToken();
        Department newDepartment = createDepartment();

        mockMvc.perform(get("/api/v1/doctors/profile")
                        .header("Authorization", "Bearer " + doctor.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(doctor.doctorProfileId()))
                .andExpect(jsonPath("$.userId").value(doctor.userId()))
                .andExpect(jsonPath("$.email").value(doctor.email()))
                .andExpect(jsonPath("$.role").value("DOCTOR"));

        Map<String, Object> update = new LinkedHashMap<>();
        update.put("departmentId", newDepartment.getId());
        update.put("specialty", "Internal Medicine");
        update.put("averageConsultationMinutes", 25);
        mockMvc.perform(put("/api/v1/doctors/profile")
                        .header("Authorization", "Bearer " + doctor.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").value(newDepartment.getId()))
                .andExpect(jsonPath("$.specialty").value("Internal Medicine"))
                .andExpect(jsonPath("$.averageConsultationMinutes").value(25));

        mockMvc.perform(get("/api/v1/doctors/{doctorId}/profile", otherDoctor.doctorProfileId())
                        .header("Authorization", "Bearer " + doctor.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/v1/doctors/profile")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/v1/doctors/profile")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/admin/doctors/{doctorId}/profile", otherDoctor.doctorProfileId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(otherDoctor.doctorProfileId()));
        mockMvc.perform(put("/api/v1/admin/doctors/{doctorId}/profile", otherDoctor.doctorProfileId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(update)))
                .andExpect(status().isOk());
    }

    @Test
    void profileUpdateCannotChangeRole() throws Exception {
        String adminToken = createAdminToken();
        DoctorAccount doctor = createDoctorToken();
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("departmentId", doctor.departmentId());
        update.put("specialty", "Neurology");
        update.put("averageConsultationMinutes", 30);
        update.put("role", "ADMIN");

        mockMvc.perform(put("/api/v1/admin/doctors/{doctorId}/profile", doctor.doctorProfileId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(update)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));

        User user = userRepository.findById(doctor.userId()).orElseThrow();
        assertThat(user.getRole()).isEqualTo(UserRole.DOCTOR);
    }

    private void createDoctor(
            String adminToken,
            String email,
            String license,
            Long departmentId) throws Exception {
        mockMvc.perform(post("/api/v1/admin/doctors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                doctorRequest(email, license, departmentId))))
                .andExpect(status().isCreated());
    }

    private String createAdminToken() {
        User admin = userRepository.saveAndFlush(new User(
                UserRole.ADMIN,
                "HTTP Test Admin",
                uniqueEmail(),
                passwordEncoder.encode("StrongPassword@123"),
                "+1-555-0100"));
        return jwtService.issueAccessToken(admin);
    }

    private String createPatientToken() {
        return authService.register(new RegisterRequest(
                "HTTP Test Patient",
                uniqueEmail(),
                "StrongPassword@123",
                "+1-555-0100",
                LocalDate.of(1990, 1, 15),
                null,
                null,
                null)).accessToken();
    }

    private DoctorAccount createDoctorToken() {
        Department department = createDepartment();
        User doctor = userRepository.saveAndFlush(new User(
                UserRole.DOCTOR,
                "HTTP Test Doctor",
                uniqueEmail(),
                passwordEncoder.encode("StrongPassword@123"),
                "+1-555-0100"));
        DoctorProfile profile = doctorProfileRepository.saveAndFlush(new DoctorProfile(
                doctor,
                department,
                "Cardiology",
                uniqueLicense(),
                20));
        return new DoctorAccount(
                doctor.getId(),
                profile.getId(),
                department.getId(),
                doctor.getEmail(),
                jwtService.issueAccessToken(doctor));
    }

    private Department createDepartment() {
        return departmentRepository.saveAndFlush(new Department(
                "Department " + UUID.randomUUID(),
                "Test department"));
    }

    private static Map<String, Object> doctorRequest(
            String email,
            String license,
            Long departmentId) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("email", email);
        request.put("password", "StrongPassword@123");
        request.put("fullName", "Created Doctor");
        request.put("phone", "+1-555-0100");
        request.put("departmentId", departmentId);
        request.put("specialty", "Cardiology");
        request.put("licenseNumber", license);
        request.put("averageConsultationMinutes", 20);
        return request;
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.test";
    }

    private static String uniqueLicense() {
        return "LIC-" + UUID.randomUUID();
    }

    private record DoctorAccount(
            Long userId,
            Long doctorProfileId,
            Long departmentId,
            String email,
            String accessToken) {
    }
}
