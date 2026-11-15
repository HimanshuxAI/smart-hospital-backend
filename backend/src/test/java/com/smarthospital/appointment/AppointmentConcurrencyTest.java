package com.smarthospital.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarthospital.ClinicalTestSupport;
import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.queue.QueueEntryRepository;
import com.smarthospital.schedule.ScheduleSlot;
import com.smarthospital.schedule.ScheduleSlotRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AppointmentConcurrencyTest extends ClinicalTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentBookingsOfOneSlotProduceExactlyOneAppointment() throws Exception {
        TestPatient firstPatient = createPatient();
        TestPatient secondPatient = createPatient();
        TestDoctor doctor = createDoctor();
        ScheduleSlot slot = createSlot(doctor, "2030-01-15T10:00:00Z");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Integer> first = executor.submit(() -> book(
                    firstPatient.accessToken(), slot.getId(), ready, start));
            Future<Integer> second = executor.submit(() -> book(
                    secondPatient.accessToken(), slot.getId(), ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(appointmentRepository.findBySlot_IdAndActiveMarker(slot.getId(), 1))
                    .isPresent();
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from appointments where slot_id = ?",
                    Integer.class,
                    slot.getId()))
                    .isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentOverlappingBookingsForOnePatientProduceExactlyOneAppointment() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        ScheduleSlot firstSlot = createSlot(doctor, "2030-01-15T10:00:00Z");
        ScheduleSlot secondSlot = createSlot(doctor, "2030-01-15T10:15:00Z");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Integer> first = executor.submit(() -> book(
                    patient.accessToken(), firstSlot.getId(), ready, start));
            Future<Integer> second = executor.submit(() -> book(
                    patient.accessToken(), secondSlot.getId(), ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(appointmentRepository.findByPatient_IdAndAppointmentDateOrderByIdAsc(
                    patient.patientId(),
                    TODAY)).hasSize(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentCheckInsAllocateDistinctDailyTokens() throws Exception {
        TestPatient firstPatient = createPatient();
        TestPatient secondPatient = createPatient();
        TestDoctor doctor = createDoctor();
        ScheduleSlot firstSlot = createSlot(doctor, "2030-01-15T08:05:00Z");
        ScheduleSlot secondSlot = createSlot(doctor, "2030-01-15T08:20:00Z");
        Appointment firstAppointment = book(firstPatient, doctor, firstSlot);
        Appointment secondAppointment = book(secondPatient, doctor, secondSlot);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Integer> first = executor.submit(() -> checkIn(
                    firstPatient.accessToken(), firstAppointment.getId(), ready, start));
            Future<Integer> second = executor.submit(() -> checkIn(
                    secondPatient.accessToken(), secondAppointment.getId(), ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactly(201, 201);
            assertThat(queueEntryRepository.findCanonicalQueueForDoctor(
                            doctor.doctorId(),
                            TODAY))
                    .extracting(entry -> entry.getTokenNumber())
                    .containsExactly(1, 2);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentDuplicateCheckInsCreateOnlyOneQueueEntry() throws Exception {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        ScheduleSlot slot = createSlot(doctor, "2030-01-15T08:30:00Z");
        Appointment appointment = book(patient, doctor, slot);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Integer> first = executor.submit(() -> checkIn(
                    patient.accessToken(), appointment.getId(), ready, start));
            Future<Integer> second = executor.submit(() -> checkIn(
                    patient.accessToken(), appointment.getId(), ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(queueEntryRepository.findByAppointment_Id(appointment.getId())).isPresent();
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from queue_entries where appointment_id = ?",
                    Integer.class,
                    appointment.getId()))
                    .isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private int book(
            String token,
            Long slotId,
            CountDownLatch ready,
            CountDownLatch start) throws Exception {
        ready.countDown();
        await(start);
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotId\":" + slotId + ",\"visitReason\":\"Concurrent booking\"}"))
                .andReturn();
        return result.getResponse().getStatus();
    }

    private int checkIn(
            String token,
            Long appointmentId,
            CountDownLatch ready,
            CountDownLatch start) throws Exception {
        ready.countDown();
        await(start);
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/appointments/" + appointmentId + "/check-in")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        return result.getResponse().getStatus();
    }

    private Appointment book(TestPatient patient, TestDoctor doctor, ScheduleSlot slot) throws Exception {
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + patient.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotId\":" + slot.getId()
                                + ",\"doctorId\":" + doctor.doctorId()
                                + ",\"visitReason\":\"Concurrent setup\"}"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        return appointmentRepository.findBySlot_IdAndActiveMarker(slot.getId(), 1).orElseThrow();
    }

    private ScheduleSlot createSlot(TestDoctor doctor, String startsAt) {
        DoctorProfile profile = doctorProfileRepository.findById(doctor.doctorId()).orElseThrow();
        Instant start = Instant.parse(startsAt);
        return scheduleSlotRepository.saveAndFlush(new ScheduleSlot(
                profile,
                start,
                start.plusSeconds(1800)));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for concurrency latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for concurrency latch", exception);
        }
    }
}
