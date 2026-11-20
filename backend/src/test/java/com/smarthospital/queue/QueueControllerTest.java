package com.smarthospital.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthospital.appointment.Appointment;
import com.smarthospital.appointment.AppointmentRepository;
import com.smarthospital.appointment.AppointmentStatus;
import com.smarthospital.schedule.ScheduleSlot;
import com.smarthospital.schedule.ScheduleSlotRepository;
import com.smarthospital.schedule.ScheduleSlotStatus;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class QueueControllerTest extends QueueTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void doctorQueueUsesCanonicalPriorityArrivalAndIdOrdering() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        QueueEntry normalOld = createQueueEntry(
                patient,
                doctor,
                EmergencyTier.NORMAL,
                Instant.parse("2030-01-15T08:00:00Z"));
        QueueEntry critical = createQueueEntry(
                patient,
                doctor,
                EmergencyTier.CRITICAL,
                Instant.parse("2030-01-15T08:02:00Z"));
        QueueEntry normalNew = createQueueEntry(
                patient,
                doctor,
                EmergencyTier.NORMAL,
                Instant.parse("2030-01-15T08:01:00Z"));
        QueueEntry high = createQueueEntry(
                patient,
                doctor,
                EmergencyTier.HIGH,
                Instant.parse("2030-01-15T08:03:00Z"));
        QueueEntry medium = createQueueEntry(
                patient,
                doctor,
                EmergencyTier.MEDIUM,
                Instant.parse("2030-01-15T08:04:00Z"));

        mockMvc.perform(get("/api/v1/queue")
                        .param("date", TODAY.toString())
                        .header("Authorization", "Bearer " + doctor.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].queueEntryId").value(critical.getId()))
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[1].queueEntryId").value(high.getId()))
                .andExpect(jsonPath("$[1].position").value(1))
                .andExpect(jsonPath("$[2].queueEntryId").value(medium.getId()))
                .andExpect(jsonPath("$[2].position").value(2))
                .andExpect(jsonPath("$[3].queueEntryId").value(normalOld.getId()))
                .andExpect(jsonPath("$[3].position").value(3))
                .andExpect(jsonPath("$[4].queueEntryId").value(normalNew.getId()))
                .andExpect(jsonPath("$[4].position").value(4));
    }

    @Test
    void patientQueueContainsOnlyThatPatientsEntry() throws Exception {
        TestPatient owner = createPatient();
        TestPatient otherPatient = createPatient();
        TestDoctor doctor = createDoctor();
        QueueEntry owned = createQueueEntry(
                owner,
                doctor,
                EmergencyTier.NORMAL,
                Instant.parse("2030-01-15T08:00:00Z"));
        createQueueEntry(
                otherPatient,
                doctor,
                EmergencyTier.HIGH,
                Instant.parse("2030-01-15T08:01:00Z"));

        mockMvc.perform(get("/api/v1/queue")
                        .param("date", TODAY.toString())
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].queueEntryId").value(owned.getId()))
                .andExpect(jsonPath("$[0].patientId").value(owner.patientId()));
    }

    @Test
    void assignedDoctorCanReadEntryButAnotherDoctorAndPatientCannot() throws Exception {
        TestPatient owner = createPatient();
        TestPatient otherPatient = createPatient();
        TestDoctor assignedDoctor = createDoctor();
        TestDoctor otherDoctor = createDoctor();
        QueueEntry entry = createQueueEntry(
                owner,
                assignedDoctor,
                EmergencyTier.NORMAL,
                Instant.parse("2030-01-15T08:00:00Z"));

        mockMvc.perform(get("/api/v1/queue/{entryId}", entry.getId())
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueEntryId").value(entry.getId()))
                .andExpect(jsonPath("$.triageLevel").value("NORMAL"))
                .andExpect(jsonPath("$.queueStatus").value("WAITING"));

        mockMvc.perform(get("/api/v1/queue/{entryId}", entry.getId())
                        .header("Authorization", "Bearer " + otherPatient.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/queue/{entryId}", entry.getId())
                        .header("Authorization", "Bearer " + otherDoctor.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void doctorQueueIncludesCalledEntryWhileWaitingPositionsStayZeroBased() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        QueueEntry called = createQueueEntry(
                patient,
                doctor,
                EmergencyTier.NORMAL,
                Instant.parse("2030-01-15T08:00:00Z"));
        QueueEntry waiting = createQueueEntry(
                patient,
                doctor,
                EmergencyTier.NORMAL,
                Instant.parse("2030-01-15T08:01:00Z"));

        mockMvc.perform(post("/api/v1/queue/{entryId}/call", called.getId())
                        .header("Authorization", "Bearer " + doctor.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueStatus").value("CALLED"));

        mockMvc.perform(get("/api/v1/queue")
                        .param("date", TODAY.toString())
                        .header("Authorization", "Bearer " + doctor.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].queueEntryId").value(called.getId()))
                .andExpect(jsonPath("$[0].queueStatus").value("CALLED"))
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[1].queueEntryId").value(waiting.getId()))
                .andExpect(jsonPath("$[1].queueStatus").value("WAITING"))
                .andExpect(jsonPath("$[1].position").value(0));
    }

    @Test
    void adminCanReadAndClassifyAnyDoctorsQueue() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        TestAdmin admin = createAdmin();
        QueueEntry entry = createQueueEntry(
                patient,
                doctor,
                EmergencyTier.NORMAL,
                Instant.parse("2030-01-15T08:00:00Z"));

        mockMvc.perform(get("/api/v1/queue")
                        .param("doctorId", doctor.doctorId().toString())
                        .param("date", TODAY.toString())
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].queueEntryId").value(entry.getId()));

        mockMvc.perform(post("/api/v1/queue/{entryId}/classify", entry.getId())
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "triageLevel", "CRITICAL",
                                "reason", "Administrator emergency classification"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.triageLevel").value("CRITICAL"));
    }

    @Test
    void patientCannotMutateQueueButAnotherDoctorCannotClassify() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor assignedDoctor = createDoctor();
        TestDoctor otherDoctor = createDoctor();
        QueueEntry entry = createQueueEntry(
                patient,
                assignedDoctor,
                EmergencyTier.NORMAL,
                Instant.parse("2030-01-15T08:00:00Z"));

        mockMvc.perform(post("/api/v1/queue/{entryId}/classify", entry.getId())
                        .header("Authorization", "Bearer " + patient.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "triageLevel", "HIGH",
                                "reason", "Patient cannot triage"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/v1/queue/{entryId}/classify", entry.getId())
                        .header("Authorization", "Bearer " + otherDoctor.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "triageLevel", "HIGH",
                                "reason", "Wrong doctor"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void classificationRequiresReasonAndWritesPriorityAudit() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        QueueEntry entry = createQueueEntry(
                patient,
                doctor,
                EmergencyTier.NORMAL,
                Instant.parse("2030-01-15T08:00:00Z"));

        mockMvc.perform(post("/api/v1/queue/{entryId}/classify", entry.getId())
                        .header("Authorization", "Bearer " + doctor.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "triageLevel", "HIGH",
                                "reason", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/v1/queue/{entryId}/classify", entry.getId())
                        .header("Authorization", "Bearer " + doctor.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "triageLevel", "HIGH",
                                "reason", "Severe symptoms"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.triageLevel").value("HIGH"))
                .andExpect(jsonPath("$.queueStatus").value("WAITING"))
                .andExpect(jsonPath("$.position").value(0));

        Map<String, Object> audit = jdbcTemplate.queryForMap(
                "select operation, previous_value, new_value, reason, actor_user_id, created_at "
                        + "from queue_audits where queue_entry_id = ?",
                entry.getId());
        assertThat(audit.get("operation")).isEqualTo("PRIORITY_OVERRIDE");
        assertThat(audit.get("previous_value")).isEqualTo("NORMAL");
        assertThat(audit.get("new_value")).isEqualTo("HIGH");
        assertThat(audit.get("reason")).isEqualTo("Severe symptoms");
        assertThat(((Number) audit.get("actor_user_id")).longValue())
                .isEqualTo(doctor.userId());
        assertThat(audit.get("created_at")).isNotNull();
    }

    @Test
    void overrideEndpointRequiresReasonAndAuditsPriorityChange() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        QueueEntry entry = createQueueEntry(
                patient,
                doctor,
                EmergencyTier.NORMAL,
                Instant.parse("2030-01-15T08:00:00Z"));

        mockMvc.perform(post("/api/v1/queue/{entryId}/override", entry.getId())
                        .header("Authorization", "Bearer " + doctor.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "triageLevel", "CRITICAL",
                                "reason", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/v1/queue/{entryId}/override", entry.getId())
                        .header("Authorization", "Bearer " + doctor.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "triageLevel", "CRITICAL",
                                "reason", "Immediate emergency"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.triageLevel").value("CRITICAL"));

        Integer auditCount = jdbcTemplate.queryForObject(
                "select count(*) from queue_audits where queue_entry_id = ? "
                        + "and operation = 'PRIORITY_OVERRIDE'",
                Integer.class,
                entry.getId());
        assertThat(auditCount).isEqualTo(1);
    }

    @Test
    void callRejectsNonCanonicalEntryThenCallsCanonicalEntry() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        QueueEntry normal = createQueueEntry(
                patient,
                doctor,
                EmergencyTier.NORMAL,
                Instant.parse("2030-01-15T08:00:00Z"));
        QueueEntry critical = createQueueEntry(
                patient,
                doctor,
                EmergencyTier.CRITICAL,
                Instant.parse("2030-01-15T08:01:00Z"));

        mockMvc.perform(post("/api/v1/queue/{entryId}/call", normal.getId())
                        .header("Authorization", "Bearer " + doctor.accessToken()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_QUEUE_TRANSITION"));

        mockMvc.perform(post("/api/v1/queue/{entryId}/call", critical.getId())
                        .header("Authorization", "Bearer " + doctor.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueStatus").value("CALLED"));
    }

    @Test
    void validCallServeCompleteProgressesQueueAppointmentAndSlot() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        QueueEntry entry = createQueueEntry(
                patient,
                doctor,
                EmergencyTier.NORMAL,
                Instant.parse("2030-01-15T08:00:00Z"));

        mockMvc.perform(post("/api/v1/queue/{entryId}/serve", entry.getId())
                        .header("Authorization", "Bearer " + doctor.accessToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_QUEUE_TRANSITION"));

        mockMvc.perform(post("/api/v1/queue/{entryId}/call", entry.getId())
                        .header("Authorization", "Bearer " + doctor.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueStatus").value("CALLED"));

        mockMvc.perform(post("/api/v1/queue/{entryId}/serve", entry.getId())
                        .header("Authorization", "Bearer " + doctor.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueStatus").value("IN_CONSULTATION"));

        mockMvc.perform(post("/api/v1/queue/{entryId}/complete", entry.getId())
                        .header("Authorization", "Bearer " + doctor.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueStatus").value("COMPLETED"));

        Appointment appointment = appointmentRepository.findById(
                entry.getAppointment().getId()).orElseThrow();
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        ScheduleSlot slot = scheduleSlotRepository.findById(
                appointment.getSlot().getId()).orElseThrow();
        assertThat(slot.getStatus()).isEqualTo(ScheduleSlotStatus.COMPLETED);
    }

    @Test
    void skipCanReturnToWaitingAndWaitingOrCalledCanBeCancelled() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        QueueEntry skipped = createQueueEntry(
                patient,
                doctor,
                EmergencyTier.NORMAL,
                Instant.parse("2030-01-15T08:00:00Z"));

        mockMvc.perform(post("/api/v1/queue/{entryId}/skip", skipped.getId())
                        .header("Authorization", "Bearer " + doctor.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "reason", "Patient not present"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueStatus").value("SKIPPED"));

        mockMvc.perform(post("/api/v1/queue/{entryId}/return-to-waiting", skipped.getId())
                        .header("Authorization", "Bearer " + doctor.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueStatus").value("WAITING"));

        mockMvc.perform(post("/api/v1/queue/{entryId}/cancel", skipped.getId())
                        .header("Authorization", "Bearer " + doctor.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "reason", "Patient withdrew from the queue"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueStatus").value("CANCELLED"));

        mockMvc.perform(post("/api/v1/queue/{entryId}/cancel", skipped.getId())
                        .header("Authorization", "Bearer " + doctor.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "reason", "Already cancelled"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_QUEUE_TRANSITION"));
    }
}
