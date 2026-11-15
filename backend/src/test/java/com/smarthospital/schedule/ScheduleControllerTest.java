package com.smarthospital.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthospital.ClinicalTestSupport;
import com.smarthospital.department.Department;
import com.smarthospital.doctor.DoctorProfile;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ScheduleControllerTest extends ClinicalTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Test
    void adminCreatesValidSlotsAndRejectsInvalidOrOverlappingSlots() throws Exception {
        TestAdmin admin = createAdmin();
        TestDoctor doctor = createDoctor();

        mockMvc.perform(post("/api/v1/admin/slots")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "doctorId", doctor.doctorId(),
                                "startsAt", "2030-01-15T10:00:00Z",
                                "endsAt", "2030-01-15T10:30:00Z",
                                "reasonLabel", "Elective consultation"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.doctorId").value(doctor.doctorId()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.reasonLabel").value("Elective consultation"));

        mockMvc.perform(post("/api/v1/admin/slots")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "doctorId", doctor.doctorId(),
                                "startsAt", "2030-01-15T10:15:00Z",
                                "endsAt", "2030-01-15T10:45:00Z"))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("SLOT_OVERLAP"));

        mockMvc.perform(post("/api/v1/admin/slots")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "doctorId", doctor.doctorId(),
                                "startsAt", "2030-01-15T11:00:00Z",
                                "endsAt", "2030-01-15T10:30:00Z"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SLOT_TIME_INVALID"));

        mockMvc.perform(post("/api/v1/admin/slots")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "doctorId", doctor.doctorId(),
                                "startsAt", "2030-01-14T10:00:00Z",
                                "endsAt", "2030-01-14T10:30:00Z"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_START_NOT_FUTURE"));
    }

    @Test
    void inactiveDoctorCannotReceiveSlotsAndOnlyAdminCanCreateThem() throws Exception {
        TestAdmin admin = createAdmin();
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        DoctorProfile inactiveDoctor = doctorProfileRepository.findById(doctor.doctorId())
                .orElseThrow();
        inactiveDoctor.setActive(false);
        doctorProfileRepository.saveAndFlush(inactiveDoctor);

        mockMvc.perform(post("/api/v1/admin/slots")
                        .header("Authorization", "Bearer " + patient.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "doctorId", doctor.doctorId(),
                                "startsAt", "2030-01-15T10:00:00Z",
                                "endsAt", "2030-01-15T10:30:00Z"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/slots")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "doctorId", doctor.doctorId(),
                                "startsAt", "2030-01-15T10:00:00Z",
                                "endsAt", "2030-01-15T10:30:00Z"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DOCTOR_INACTIVE"));
    }

    @Test
    void adminCancelsAnOpenSlot() throws Exception {
        TestAdmin admin = createAdmin();
        TestDoctor doctor = createDoctor();
        Instant startsAt = Instant.parse("2030-01-15T12:00:00Z");
        ScheduleSlot slot = scheduleSlotRepository.saveAndFlush(new ScheduleSlot(
                doctorProfileRepository.findById(doctor.doctorId()).orElseThrow(),
                startsAt,
                startsAt.plusSeconds(1800),
                "Cancelled clinic"));

        mockMvc.perform(post("/api/v1/admin/slots/{slotId}/cancel", slot.getId())
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(scheduleSlotRepository.findById(slot.getId()).orElseThrow().isActive())
                .isFalse();
    }

    @Test
    void patientSeesOnlyUnbookedFutureSlotsForTheRequestedDate() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        var doctorProfile = doctorProfileRepository.findById(doctor.doctorId()).orElseThrow();

        ScheduleSlot visible = scheduleSlotRepository.saveAndFlush(new ScheduleSlot(
                doctorProfile,
                Instant.parse("2030-01-15T10:00:00Z"),
                Instant.parse("2030-01-15T10:30:00Z")));
        ScheduleSlot booked = scheduleSlotRepository.saveAndFlush(new ScheduleSlot(
                doctorProfile,
                Instant.parse("2030-01-15T11:00:00Z"),
                Instant.parse("2030-01-15T11:30:00Z")));
        booked.markBooked();
        scheduleSlotRepository.saveAndFlush(booked);
        scheduleSlotRepository.saveAndFlush(new ScheduleSlot(
                doctorProfile,
                Instant.parse("2030-01-15T07:00:00Z"),
                Instant.parse("2030-01-15T07:30:00Z")));
        scheduleSlotRepository.saveAndFlush(new ScheduleSlot(
                doctorProfile,
                Instant.parse("2030-01-16T10:00:00Z"),
                Instant.parse("2030-01-16T10:30:00Z")));

        mockMvc.perform(get("/api/v1/doctors/{doctorId}/slots", doctor.doctorId())
                        .param("date", "2030-01-15")
                        .header("Authorization", "Bearer " + patient.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(visible.getId()))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void doctorProfileCannotBrowsePatientSlotEndpoint() throws Exception {
        TestDoctor doctor = createDoctor();

        mockMvc.perform(get("/api/v1/doctors/{doctorId}/slots", doctor.doctorId())
                        .param("date", "2030-01-15")
                        .header("Authorization", "Bearer " + doctor.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
