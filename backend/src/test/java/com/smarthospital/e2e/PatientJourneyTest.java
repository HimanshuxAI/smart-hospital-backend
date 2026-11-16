package com.smarthospital.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthospital.ClinicalTestSupport;
import com.smarthospital.schedule.ScheduleSlot;
import com.smarthospital.schedule.ScheduleSlotRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class PatientJourneyTest extends ClinicalTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Test
    void completePatientClinicalJourney() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();

        // 1. Create schedule slot
        ScheduleSlot slot = scheduleSlotRepository.saveAndFlush(new ScheduleSlot(
                doctorProfileRepository.findById(doctor.doctorId()).orElseThrow(),
                NOW.plusSeconds(300),
                NOW.plusSeconds(2100)));

        // 2. Patient books the appointment
        MvcResult bookResult = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + patient.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "slotId", slot.getId(),
                                "visitReason", "Cardiology routine evaluation"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andReturn();

        long appointmentId = objectMapper.readTree(bookResult.getResponse().getContentAsString())
                .get("id").asLong();

        // 3. Patient checks in to generate digital queue token
        MvcResult checkInResult = mockMvc.perform(post("/api/v1/appointments/{id}/check-in", appointmentId)
                        .header("Authorization", "Bearer " + patient.accessToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenNumber").isNumber())
                .andExpect(jsonPath("$.queueStatus").value("WAITING"))
                .andReturn();

        long queueEntryId = objectMapper.readTree(checkInResult.getResponse().getContentAsString())
                .get("queueEntryId").asLong();

        // 4. Doctor triages the patient to HIGH priority
        mockMvc.perform(post("/api/v1/queue/{id}/classify", queueEntryId)
                        .header("Authorization", "Bearer " + doctor.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "triageLevel", "HIGH",
                                "reason", "Patient reported severe acute discomfort"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.triageLevel").value("HIGH"));

        // 5. Doctor calls patient into consultation
        mockMvc.perform(post("/api/v1/queue/{id}/call", queueEntryId)
                        .header("Authorization", "Bearer " + doctor.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueStatus").value("CALLED"));

        // 6. Doctor begins consultation
        mockMvc.perform(post("/api/v1/queue/{id}/serve", queueEntryId)
                        .header("Authorization", "Bearer " + doctor.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueStatus").value("IN_CONSULTATION"));

        // 7. Doctor completes consultation
        mockMvc.perform(post("/api/v1/queue/{id}/complete", queueEntryId)
                        .header("Authorization", "Bearer " + doctor.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueStatus").value("COMPLETED"));

        // 8. Patient verifies waiting time prediction endpoint with heuristic fallback resilience
        mockMvc.perform(get("/api/v1/queue/{id}/waiting-time", queueEntryId)
                        .header("Authorization", "Bearer " + patient.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").isNotEmpty())
                .andExpect(jsonPath("$.estimatedWaitMinutes").isNumber());
    }
}
