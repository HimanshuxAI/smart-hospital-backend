package com.smarthospital.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarthospital.appointment.Appointment;
import com.smarthospital.appointment.AppointmentRepository;
import com.smarthospital.department.Department;
import com.smarthospital.department.DepartmentRepository;
import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.doctor.DoctorProfileRepository;
import com.smarthospital.patient.PatientProfile;
import com.smarthospital.patient.PatientProfileRepository;
import com.smarthospital.schedule.ScheduleSlot;
import com.smarthospital.schedule.ScheduleSlotRepository;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class PersistenceConcurrencyTest extends MySqlIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void patientLockSerializesOverlappingBookingAttempts() throws Exception {
        DoctorProfile doctor = persistDoctor();
        PatientProfile patient = persistPatient();
        Instant firstStart = Instant.parse("2032-01-01T10:00:00Z");
        Instant firstEnd = firstStart.plusSeconds(1800);
        Instant secondStart = Instant.parse("2032-01-01T10:15:00Z");
        Instant secondEnd = secondStart.plusSeconds(1800);
        ScheduleSlot firstSlot = persistSlot(doctor, firstStart, firstEnd);
        ScheduleSlot secondSlot = persistSlot(doctor, secondStart, secondEnd);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch secondAcquired = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<WriteOutcome> firstResult = executor.submit(() -> transactionTemplate.execute(status ->
                    attemptBooking(
                            patient.getId(),
                            doctor.getId(),
                            firstSlot.getId(),
                            firstStart,
                            firstEnd,
                            firstLocked,
                            releaseFirst)));
            Future<WriteOutcome> secondResult = executor.submit(() -> transactionTemplate.execute(status -> {
                secondStarted.countDown();
                await(firstLocked);
                WriteOutcome outcome = attemptBooking(
                        patient.getId(),
                        doctor.getId(),
                        secondSlot.getId(),
                        secondStart,
                        secondEnd,
                        secondAcquired,
                        null);
                return outcome;
            }));

            assertThat(firstLocked.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(secondAcquired.await(250, TimeUnit.MILLISECONDS))
                    .as("second booking must wait for the locked patient row")
                    .isFalse();

            releaseFirst.countDown();

            assertThat(List.of(
                            firstResult.get(10, TimeUnit.SECONDS),
                            secondResult.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(WriteOutcome.SUCCESS, WriteOutcome.CONFLICT);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM appointments WHERE patient_id = ?",
                    Integer.class,
                    patient.getId())).isEqualTo(1);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void doctorLockSerializesOverlappingSlotCreationAttempts() throws Exception {
        DoctorProfile doctor = persistDoctor();
        Instant firstStart = Instant.parse("2032-02-01T10:00:00Z");
        Instant firstEnd = firstStart.plusSeconds(1800);
        Instant secondStart = Instant.parse("2032-02-01T10:15:00Z");
        Instant secondEnd = secondStart.plusSeconds(1800);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch secondAcquired = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<WriteOutcome> firstResult = executor.submit(() -> transactionTemplate.execute(status ->
                    attemptSlotCreation(
                            doctor.getId(),
                            firstStart,
                            firstEnd,
                            firstLocked,
                            releaseFirst)));
            Future<WriteOutcome> secondResult = executor.submit(() -> transactionTemplate.execute(status -> {
                secondStarted.countDown();
                await(firstLocked);
                WriteOutcome outcome = attemptSlotCreation(
                        doctor.getId(),
                        secondStart,
                        secondEnd,
                        secondAcquired,
                        null);
                return outcome;
            }));

            assertThat(firstLocked.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(secondAcquired.await(250, TimeUnit.MILLISECONDS))
                    .as("second slot creation must wait for the locked doctor row")
                    .isFalse();

            releaseFirst.countDown();

            assertThat(List.of(
                            firstResult.get(10, TimeUnit.SECONDS),
                            secondResult.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(WriteOutcome.SUCCESS, WriteOutcome.CONFLICT);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM schedule_slots WHERE doctor_id = ?",
                    Integer.class,
                    doctor.getId())).isEqualTo(1);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    private WriteOutcome attemptBooking(
            Long patientId,
            Long doctorId,
            Long slotId,
            Instant proposedStart,
            Instant proposedEnd,
            CountDownLatch acquired,
            CountDownLatch release) {
        PatientProfile patient = patientProfileRepository.findByIdForUpdate(patientId).orElseThrow();
        acquired.countDown();
        if (release != null) {
            await(release);
        }
        if (!appointmentRepository.findActiveOverlappingForPatient(
                        patientId,
                        proposedStart,
                        proposedEnd)
                .isEmpty()) {
            return WriteOutcome.CONFLICT;
        }

        DoctorProfile doctor = doctorProfileRepository.findById(doctorId).orElseThrow();
        ScheduleSlot slot = scheduleSlotRepository.findById(slotId).orElseThrow();
        appointmentRepository.saveAndFlush(new Appointment(
                patient,
                doctor,
                slot,
                LocalDate.ofInstant(proposedStart, java.time.ZoneOffset.UTC),
                "Synthetic concurrent booking"));
        return WriteOutcome.SUCCESS;
    }

    private WriteOutcome attemptSlotCreation(
            Long doctorId,
            Instant proposedStart,
            Instant proposedEnd,
            CountDownLatch acquired,
            CountDownLatch release) {
        DoctorProfile doctor = doctorProfileRepository.findByIdForUpdate(doctorId).orElseThrow();
        acquired.countDown();
        if (release != null) {
            await(release);
        }
        if (!scheduleSlotRepository.findActiveOverlappingByDoctor(
                        doctorId,
                        proposedStart,
                        proposedEnd)
                .isEmpty()) {
            return WriteOutcome.CONFLICT;
        }

        scheduleSlotRepository.saveAndFlush(new ScheduleSlot(doctor, proposedStart, proposedEnd));
        return WriteOutcome.SUCCESS;
    }

    private User persistUser() {
        String unique = UUID.randomUUID().toString();
        return userRepository.saveAndFlush(new User(
                UserRole.PATIENT,
                "Synthetic User",
                "patient-" + unique + "@example.test",
                "not-used-in-this-test",
                null));
    }

    private PatientProfile persistPatient() {
        return patientProfileRepository.saveAndFlush(new PatientProfile(
                persistUser(),
                LocalDate.of(1990, 1, 15)));
    }

    private DoctorProfile persistDoctor() {
        String unique = UUID.randomUUID().toString();
        User user = userRepository.saveAndFlush(new User(
                UserRole.DOCTOR,
                "Synthetic Doctor",
                "doctor-" + unique + "@example.test",
                "not-used-in-this-test",
                null));
        Department department = departmentRepository.saveAndFlush(new Department(
                "Synthetic-" + unique,
                "Synthetic department"));
        return doctorProfileRepository.saveAndFlush(new DoctorProfile(
                user,
                department,
                "General Medicine",
                "LICENSE-" + unique,
                15));
    }

    private ScheduleSlot persistSlot(DoctorProfile doctor, Instant startsAt, Instant endsAt) {
        return scheduleSlotRepository.saveAndFlush(new ScheduleSlot(doctor, startsAt, endsAt));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for test coordination latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for test coordination latch", exception);
        }
    }

    private enum WriteOutcome {
        SUCCESS,
        CONFLICT
    }
}
