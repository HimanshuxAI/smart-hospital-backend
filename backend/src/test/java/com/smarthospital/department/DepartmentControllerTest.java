package com.smarthospital.department;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthospital.ClinicalTestSupport;
import com.smarthospital.doctor.DoctorProfile;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DepartmentControllerTest extends ClinicalTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void onlyAdminCanManageDepartmentsAndDuplicateNamesHaveAStableConflictCode() throws Exception {
        TestPatient patient = createPatient();
        TestAdmin admin = createAdmin();
        String name = "Cardiology " + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/admin/departments")
                        .header("Authorization", "Bearer " + patient.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", name,
                                "description", "Heart and vascular care"))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/v1/admin/departments")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", name,
                                "description", "Heart and vascular care"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/v1/admin/departments")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", name.toUpperCase(),
                                "description", "Duplicate"))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("DEPARTMENT_NAME_ALREADY_EXISTS"));

        Department department = departmentRepository.findByNameIgnoreCase(name).orElseThrow();

        mockMvc.perform(put("/api/v1/admin/departments/{departmentId}", department.getId())
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", name + " Updated",
                                "description", "Updated description"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name + " Updated"))
                .andExpect(jsonPath("$.description").value("Updated description"));

        mockMvc.perform(delete("/api/v1/admin/departments/{departmentId}", department.getId())
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isNoContent());

        assertThat(departmentRepository.findById(department.getId()).orElseThrow().isActive())
                .isFalse();

        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", "Bearer " + patient.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + department.getId() + ")]").isEmpty());
    }

    @Test
    void departmentCanBeDeactivatedAndReactivatedWithTheActivationEndpoint() throws Exception {
        TestAdmin admin = createAdmin();
        Department department = createDepartment();

        mockMvc.perform(patch("/api/v1/admin/departments/{departmentId}/activation", department.getId())
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/api/v1/admin/departments/{departmentId}/activation", department.getId())
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("active", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void doctorDirectoryExcludesInactiveDoctorsUsersAndDepartments() throws Exception {
        TestPatient patient = createPatient();

        Department activeDepartment = createDepartment();
        TestDoctor visibleDoctor = createDoctor(activeDepartment);

        Department inactiveDepartment = createDepartment();
        inactiveDepartment.setActive(false);
        departmentRepository.saveAndFlush(inactiveDepartment);
        TestDoctor doctorInInactiveDepartment = createDoctor(inactiveDepartment);

        TestDoctor inactiveProfile = createDoctor(activeDepartment);
        DoctorProfile inactiveProfileEntity = doctorProfileRepository.findById(inactiveProfile.doctorId())
                .orElseThrow();
        inactiveProfileEntity.setActive(false);
        doctorProfileRepository.saveAndFlush(inactiveProfileEntity);

        TestDoctor inactiveUser = createDoctor(activeDepartment);
        inactiveUserAccount(inactiveUser.userId());

        mockMvc.perform(get("/api/v1/doctors")
                        .header("Authorization", "Bearer " + patient.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + visibleDoctor.doctorId() + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + doctorInInactiveDepartment.doctorId() + ")]").isEmpty())
                .andExpect(jsonPath("$[?(@.id == " + inactiveProfile.doctorId() + ")]").isEmpty())
                .andExpect(jsonPath("$[?(@.id == " + inactiveUser.doctorId() + ")]").isEmpty());
    }

    private void inactiveUserAccount(Long userId) {
        var user = userRepository.findById(userId).orElseThrow();
        user.setActive(false);
        userRepository.saveAndFlush(user);
    }
}
