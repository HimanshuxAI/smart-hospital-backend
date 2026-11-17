package com.smarthospital.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthospital.ClinicalTestSupport;
import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.queue.QueueEntryRepository;
import com.smarthospital.schedule.ScheduleSlot;
import com.smarthospital.schedule.ScheduleSlotRepository;
import com.smarthospital.schedule.ScheduleSlotStatus;
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
class AppointmentControllerTest extends ClinicalTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Test
    void patientBooksOpenSlotAndDerivedDoctorIsUsed() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        ScheduleSlot slot = createSlot(doctor, "2030-01-15T10:00:00Z", "2030-01-15T10:30:00Z");

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + patient.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "slotId", slot.getId(),
                                "doctorId", doctor.doctorId(),
                                "visitReason", "Persistent cough"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(patient.patientId()))
                .andExpect(jsonPath("$.doctorId").value(doctor.doctorId()))
                .andExpect(jsonPath("$.slotId").value(slot.getId()))
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.visitReason").value("Persistent cough"));

        assertThat(scheduleSlotRepository.findById(slot.getId()).orElseThrow().getStatus())
                .isEqualTo(ScheduleSlotStatus.BOOKED);
    }

    @Test
    void submittedDoctorCannotRedirectBookingToAnotherDoctor() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        TestDoctor otherDoctor = createDoctor();
        ScheduleSlot slot = createSlot(doctor, "2030-01-15T10:00:00Z", "2030-01-15T10:30:00Z");

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + patient.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "slotId", slot.getId(),
                                "doctorId", otherDoctor.doctorId(),
                                "visitReason", "Misleading doctor"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DOCTOR_SLOT_MISMATCH"));

        assertThat(appointmentRepository.findBySlot_IdAndActiveMarker(slot.getId(), 1)).isEmpty();
    }

    @Test
    void bookedSlotReturnsStableConflictCode() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        ScheduleSlot slot = createSlot(doctor, "2030-01-15T10:00:00Z", "2030-01-15T10:30:00Z");
        book(patient, doctor, slot);

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + patient.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "slotId", slot.getId(),
                                "visitReason", "Second attempt"))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("SLOT_ALREADY_BOOKED"));
    }

    @Test
    void overlappingPatientAppointmentsAreRejected() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        ScheduleSlot first = createSlot(doctor, "2030-01-15T10:00:00Z", "2030-01-15T10:30:00Z");
        ScheduleSlot second = createSlot(doctor, "2030-01-15T10:15:00Z", "2030-01-15T10:45:00Z");
        book(patient, doctor, first);

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + patient.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "slotId", second.getId(),
                                "visitReason", "Overlapping visit"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PATIENT_APPOINTMENT_OVERLAP"));

        assertThat(appointmentRepository.findByPatient_IdAndAppointmentDateOrderByIdAsc(
                patient.patientId(),
                TODAY)).hasSize(1);
    }

    @Test
    void ownerCanCancelAndCancellationReleasesSlotAndRecordsMetadata() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        ScheduleSlot slot = createSlot(doctor, "2030-01-15T10:00:00Z", "2030-01-15T10:30:00Z");
        Appointment appointment = book(patient, doctor, slot);

        mockMvc.perform(post("/api/v1/appointments/{appointmentId}/cancel", appointment.getId())
                        .header("Authorization", "Bearer " + patient.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "reason", "Patient no longer needs the visit"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.activeMarker").doesNotExist())
                .andExpect(jsonPath("$.cancellationReason").value("Patient no longer needs the visit"))
                .andExpect(jsonPath("$.cancelledAt").exists());

        Appointment cancelled = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertThat(cancelled.getActiveMarker()).isNull();
        assertThat(scheduleSlotRepository.findById(slot.getId()).orElseThrow().getStatus())
                .isEqualTo(ScheduleSlotStatus.OPEN);
    }

    @Test
    void nonOwnerCannotCancelButAdminCanCancelAnyBookedAppointment() throws Exception {
        TestPatient owner = createPatient();
        TestPatient otherPatient = createPatient();
        TestAdmin admin = createAdmin();
        TestDoctor doctor = createDoctor();
        ScheduleSlot ownedSlot = createSlot(doctor, "2030-01-15T10:00:00Z", "2030-01-15T10:30:00Z");
        ScheduleSlot adminSlot = createSlot(doctor, "2030-01-15T11:00:00Z", "2030-01-15T11:30:00Z");
        Appointment owned = book(owner, doctor, ownedSlot);
        Appointment adminOwned = book(owner, doctor, adminSlot);

        mockMvc.perform(post("/api/v1/appointments/{appointmentId}/cancel", owned.getId())
                        .header("Authorization", "Bearer " + otherPatient.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("reason", "Not my appointment"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/v1/appointments/{appointmentId}/cancel", adminOwned.getId())
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("reason", "Administrative cancellation"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void checkInCreatesWaitingQueueEntryWithSequentialTokenAndPosition() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        ScheduleSlot first = createSlot(doctor, "2030-01-15T08:05:00Z", "2030-01-15T08:20:00Z");
        ScheduleSlot second = createSlot(doctor, "2030-01-15T08:20:00Z", "2030-01-15T08:35:00Z");
        Appointment firstAppointment = book(patient, doctor, first);
        Appointment secondAppointment = book(patient, doctor, second);

        mockMvc.perform(post("/api/v1/appointments/{appointmentId}/check-in", firstAppointment.getId())
                        .header("Authorization", "Bearer " + patient.accessToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(1))
                .andExpect(jsonPath("$.tokenNumber").value(1))
                .andExpect(jsonPath("$.queueStatus").value("WAITING"))
                .andExpect(jsonPath("$.triageLevel").value("NORMAL"))
                .andExpect(jsonPath("$.position").value(0));

        mockMvc.perform(post("/api/v1/appointments/{appointmentId}/check-in", secondAppointment.getId())
                        .header("Authorization", "Bearer " + patient.accessToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(2))
                .andExpect(jsonPath("$.position").value(1));

        assertThat(appointmentRepository.findById(firstAppointment.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentStatus.CHECKED_IN);
        assertThat(queueEntryRepository.findByAppointment_Id(firstAppointment.getId())).isPresent();
    }

    @Test
    void duplicateCheckInIsRejected() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        ScheduleSlot slot = createSlot(doctor, "2030-01-15T08:30:00Z", "2030-01-15T09:00:00Z");
        Appointment appointment = book(patient, doctor, slot);

        mockMvc.perform(post("/api/v1/appointments/{appointmentId}/check-in", appointment.getId())
                        .header("Authorization", "Bearer " + patient.accessToken()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/appointments/{appointmentId}/check-in", appointment.getId())
                        .header("Authorization", "Bearer " + patient.accessToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_CHECKED_IN"));

        assertThat(queueEntryRepository.findByAppointment_Id(appointment.getId())).isPresent();
    }

    @Test
    void earlyLateAndWrongDateCheckInsAreRejected() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        ScheduleSlot earlySlot = createSlot(doctor, "2030-01-15T09:00:00Z", "2030-01-15T09:30:00Z");
        Appointment earlyAppointment = book(patient, doctor, earlySlot);

        mockMvc.perform(post("/api/v1/appointments/{appointmentId}/check-in", earlyAppointment.getId())
                        .header("Authorization", "Bearer " + patient.accessToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHECK_IN_TOO_EARLY"));

        ScheduleSlot lateSlot = persistSlot(doctor, "2030-01-15T07:00:00Z", "2030-01-15T07:30:00Z");
        Appointment lateAppointment = persistBookedAppointment(patient, doctor, lateSlot);
        mockMvc.perform(post("/api/v1/appointments/{appointmentId}/check-in", lateAppointment.getId())
                        .header("Authorization", "Bearer " + patient.accessToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHECK_IN_TOO_LATE"));

        ScheduleSlot tomorrowSlot = createSlot(doctor, "2030-01-16T08:30:00Z", "2030-01-16T09:00:00Z");
        Appointment tomorrowAppointment = book(patient, doctor, tomorrowSlot);
        mockMvc.perform(post("/api/v1/appointments/{appointmentId}/check-in", tomorrowAppointment.getId())
                        .header("Authorization", "Bearer " + patient.accessToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_DATE_MISMATCH"));
    }

    @Test
    void assignedDoctorAndAdminMayCheckInButAnotherDoctorMayNot() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor assignedDoctor = createDoctor();
        TestDoctor otherDoctor = createDoctor();
        TestAdmin admin = createAdmin();
        ScheduleSlot slot = createSlot(assignedDoctor, "2030-01-15T08:05:00Z", "2030-01-15T08:20:00Z");
        Appointment appointment = book(patient, assignedDoctor, slot);

        mockMvc.perform(post("/api/v1/appointments/{appointmentId}/check-in", appointment.getId())
                        .header("Authorization", "Bearer " + otherDoctor.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/v1/appointments/{appointmentId}/check-in", appointment.getId())
                        .header("Authorization", "Bearer " + assignedDoctor.accessToken()))
                .andExpect(status().isCreated());

        TestPatient secondPatient = createPatient();
        ScheduleSlot adminSlot = createSlot(assignedDoctor, "2030-01-15T08:20:00Z", "2030-01-15T08:35:00Z");
        Appointment adminAppointment = book(secondPatient, assignedDoctor, adminSlot);
        mockMvc.perform(post("/api/v1/appointments/{appointmentId}/check-in", adminAppointment.getId())
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isCreated());
    }

    @Test
    void patientCannotCancelAfterCheckIn() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        ScheduleSlot slot = createSlot(doctor, "2030-01-15T08:30:00Z", "2030-01-15T09:00:00Z");
        Appointment appointment = book(patient, doctor, slot);
        mockMvc.perform(post("/api/v1/appointments/{appointmentId}/check-in", appointment.getId())
                        .header("Authorization", "Bearer " + patient.accessToken()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/appointments/{appointmentId}/cancel", appointment.getId())
                        .header("Authorization", "Bearer " + patient.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "reason", "Changed my mind after check-in"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPOINTMENT_NOT_CANCELLABLE"));
    }

    @Test
    void patientCanReadOnlyTheirOwnAppointment() throws Exception {
        TestPatient owner = createPatient();
        TestPatient otherPatient = createPatient();
        TestDoctor doctor = createDoctor();
        ScheduleSlot slot = createSlot(doctor, "2030-01-15T10:00:00Z", "2030-01-15T10:30:00Z");
        Appointment appointment = book(owner, doctor, slot);

        mockMvc.perform(get("/api/v1/appointments/{appointmentId}", appointment.getId())
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointment.getId()));

        mockMvc.perform(get("/api/v1/appointments/{appointmentId}", appointment.getId())
                        .header("Authorization", "Bearer " + otherPatient.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void patientListsOnlyOwnAppointments() throws Exception {
        TestPatient owner = createPatient();
        TestPatient otherPatient = createPatient();
        TestDoctor doctor = createDoctor();
        ScheduleSlot ownedSlot = createSlot(doctor, "2030-01-15T10:00:00Z", "2030-01-15T10:30:00Z");
        ScheduleSlot otherSlot = createSlot(doctor, "2030-01-15T11:00:00Z", "2030-01-15T11:30:00Z");
        Appointment owned = book(owner, doctor, ownedSlot);
        book(otherPatient, doctor, otherSlot);

        mockMvc.perform(get("/api/v1/appointments")
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(owned.getId()))
                .andExpect(jsonPath("$[0].patientId").value(owner.patientId()));
    }

    @Test
    void doctorListsAssignedAppointmentsAndAdminListsEveryAppointment() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor assignedDoctor = createDoctor();
        TestDoctor otherDoctor = createDoctor();
        TestAdmin admin = createAdmin();
        ScheduleSlot assignedSlot = createSlot(
                assignedDoctor,
                "2030-01-15T10:00:00Z",
                "2030-01-15T10:30:00Z");
        ScheduleSlot otherSlot = createSlot(
                otherDoctor,
                "2030-01-15T11:00:00Z",
                "2030-01-15T11:30:00Z");
        Appointment assigned = book(patient, assignedDoctor, assignedSlot);
        Appointment other = book(patient, otherDoctor, otherSlot);

        mockMvc.perform(get("/api/v1/appointments")
                        .header("Authorization", "Bearer " + assignedDoctor.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(assigned.getId()))
                .andExpect(jsonPath("$[0].doctorId").value(assignedDoctor.doctorId()));

        mockMvc.perform(get("/api/v1/appointments")
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + assigned.getId() + ")]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id == " + other.getId() + ")]").isNotEmpty());
    }

    @Test
    void appointmentListSupportsDoctorAndDateFilters() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        TestDoctor otherDoctor = createDoctor();
        TestAdmin admin = createAdmin();
        ScheduleSlot todaySlot = createSlot(doctor, "2030-01-15T10:00:00Z", "2030-01-15T10:30:00Z");
        ScheduleSlot tomorrowSlot = createSlot(doctor, "2030-01-16T10:00:00Z", "2030-01-16T10:30:00Z");
        ScheduleSlot otherDoctorSlot =
                createSlot(otherDoctor, "2030-01-15T12:00:00Z", "2030-01-15T12:30:00Z");
        book(patient, doctor, todaySlot);
        book(patient, doctor, tomorrowSlot);
        book(patient, otherDoctor, otherDoctorSlot);

        mockMvc.perform(get("/api/v1/appointments")
                        .param("date", "2030-01-15")
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.appointmentDate != '2030-01-15')]").isEmpty());

        mockMvc.perform(get("/api/v1/appointments")
                        .param("doctorId", String.valueOf(doctor.doctorId()))
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.doctorId != " + doctor.doctorId() + ")]").isEmpty());
    }

    @Test
    void anonymousUserCannotListAppointments() throws Exception {
        mockMvc.perform(get("/api/v1/appointments"))
                .andExpect(status().isUnauthorized());
    }

    private Appointment book(TestPatient patient, TestDoctor doctor, ScheduleSlot slot) throws Exception {
        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + patient.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "slotId", slot.getId(),
                                "doctorId", doctor.doctorId(),
                                "visitReason", "Clinical test visit"))))
                .andExpect(status().isCreated());
        return appointmentRepository.findBySlot_IdAndActiveMarker(slot.getId(), 1).orElseThrow();
    }

    private ScheduleSlot createSlot(TestDoctor doctor, String startsAt, String endsAt) {
        return persistSlot(doctor, startsAt, endsAt);
    }

    private ScheduleSlot persistSlot(TestDoctor doctor, String startsAt, String endsAt) {
        DoctorProfile profile = doctorProfileRepository.findById(doctor.doctorId()).orElseThrow();
        return scheduleSlotRepository.saveAndFlush(new ScheduleSlot(
                profile,
                Instant.parse(startsAt),
                Instant.parse(endsAt)));
    }

    private Appointment persistBookedAppointment(
            TestPatient patient,
            TestDoctor doctor,
            ScheduleSlot slot) {
        slot.markBooked();
        scheduleSlotRepository.saveAndFlush(slot);
        DoctorProfile profile = doctorProfileRepository.findById(doctor.doctorId()).orElseThrow();
        return appointmentRepository.saveAndFlush(new Appointment(
                patientProfileRepository.findById(patient.patientId()).orElseThrow(),
                profile,
                slot,
                slot.getStartsAt().atZone(java.time.ZoneOffset.UTC).toLocalDate(),
                "Persisted booked appointment"));
    }
}
