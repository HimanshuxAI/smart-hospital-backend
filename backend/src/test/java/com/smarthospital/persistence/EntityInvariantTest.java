package com.smarthospital.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smarthospital.appointment.Appointment;
import com.smarthospital.appointment.AppointmentRepository;
import com.smarthospital.appointment.AppointmentStatus;
import com.smarthospital.department.Department;
import com.smarthospital.department.DepartmentRepository;
import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.doctor.DoctorProfileRepository;
import com.smarthospital.patient.PatientProfile;
import com.smarthospital.patient.PatientProfileRepository;
import com.smarthospital.queue.EmergencyTier;
import com.smarthospital.queue.QueueAudit;
import com.smarthospital.queue.QueueEntry;
import com.smarthospital.queue.QueueEntryRepository;
import com.smarthospital.queue.QueueStatus;
import com.smarthospital.schedule.ScheduleSlot;
import com.smarthospital.schedule.ScheduleSlotRepository;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class EntityInvariantTest extends MySqlIntegrationTest {

    private static final AtomicInteger SLOT_SEQUENCE = new AtomicInteger();
    private static final AtomicInteger TOKEN_SEQUENCE = new AtomicInteger();

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
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appointmentStatusAndActiveMarkerStayAlignedWithMigration() {
        DoctorProfile doctor = persistDoctor();
        Appointment appointment = new Appointment(
                persistPatient(),
                doctor,
                persistSlot(doctor),
                LocalDate.now(),
                "Synthetic visit reason");

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
        assertThat(appointment.getActiveMarker()).isEqualTo(1);

        appointment.transitionTo(AppointmentStatus.CHECKED_IN);
        assertThat(appointment.getActiveMarker()).isEqualTo(1);

        appointment.transitionTo(AppointmentStatus.COMPLETED);
        assertThat(appointment.getActiveMarker()).isNull();

        appointment.transitionTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.getActiveMarker()).isNull();

        appointment.transitionTo(AppointmentStatus.NO_SHOW);
        assertThat(appointment.getActiveMarker()).isNull();
    }

    @Test
    void databaseRejectsNullActiveMarkerForActiveAppointments() {
        DoctorProfile doctor = persistDoctor();
        Appointment appointment = appointmentRepository.saveAndFlush(new Appointment(
                persistPatient(),
                doctor,
                persistSlot(doctor),
                LocalDate.now(),
                "Synthetic raw active-marker visit"));

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE appointments SET status = 'BOOKED', active_marker = NULL WHERE id = ?",
                        appointment.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_appointments_active_marker");

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE appointments SET status = 'CHECKED_IN', active_marker = NULL WHERE id = ?",
                        appointment.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_appointments_active_marker");
    }

    @Test
    void databaseRejectsNonNullActiveMarkerForTerminalAppointments() {
        DoctorProfile doctor = persistDoctor();
        Appointment appointment = appointmentRepository.saveAndFlush(new Appointment(
                persistPatient(),
                doctor,
                persistSlot(doctor),
                LocalDate.now(),
                "Synthetic raw terminal-marker visit"));

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE appointments SET status = 'COMPLETED', active_marker = 1 WHERE id = ?",
                        appointment.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_appointments_active_marker");

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE appointments SET status = 'CANCELLED', active_marker = 1 WHERE id = ?",
                        appointment.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_appointments_active_marker");

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "UPDATE appointments SET status = 'NO_SHOW', active_marker = 1 WHERE id = ?",
                        appointment.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_appointments_active_marker");
    }

    @Test
    void onlyOneActiveAppointmentCanConsumeASlot() {
        DoctorProfile doctor = persistDoctor();
        ScheduleSlot slot = persistSlot(doctor);
        PatientProfile patient = persistPatient();
        appointmentRepository.saveAndFlush(new Appointment(
                patient,
                doctor,
                slot,
                LocalDate.now(),
                "Synthetic visit reason"));

        assertThatThrownBy(() -> appointmentRepository.saveAndFlush(new Appointment(
                        patient,
                        doctor,
                        slot,
                        LocalDate.now(),
                        "Synthetic conflicting visit reason")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void terminalAppointmentsAllowHistoricalRowsForTheSameSlot() {
        DoctorProfile doctor = persistDoctor();
        ScheduleSlot slot = persistSlot(doctor);
        PatientProfile patient = persistPatient();

        Appointment active = new Appointment(patient, doctor, slot, LocalDate.now(), "Synthetic active visit");
        appointmentRepository.saveAndFlush(active);

        Appointment cancelled = new Appointment(patient, doctor, slot, LocalDate.now(), "Synthetic cancelled visit");
        cancelled.transitionTo(AppointmentStatus.CANCELLED);
        appointmentRepository.saveAndFlush(cancelled);

        Appointment noShow = new Appointment(patient, doctor, slot, LocalDate.now(), "Synthetic no-show visit");
        noShow.transitionTo(AppointmentStatus.NO_SHOW);
        appointmentRepository.saveAndFlush(noShow);

        Integer appointmentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM appointments WHERE slot_id = ?",
                Integer.class,
                slot.getId());
        assertThat(appointmentCount).isEqualTo(3);
        String persistedStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM appointments WHERE id = ?",
                String.class,
                active.getId());
        assertThat(persistedStatus).isEqualTo("BOOKED");
    }

    @Test
    void queueLookupOrdersCriticalEntriesBeforeNormalEntries() {
        DoctorProfile doctor = persistDoctor();
        PatientProfile patient = persistPatient();
        LocalDate queueDate = LocalDate.now();
        List<EmergencyTier> insertedTiers = List.of(
                EmergencyTier.NORMAL,
                EmergencyTier.CRITICAL,
                EmergencyTier.MEDIUM,
                EmergencyTier.HIGH);

        for (EmergencyTier tier : insertedTiers) {
            ScheduleSlot slot = persistSlot(doctor);
            Appointment appointment = appointmentRepository.saveAndFlush(new Appointment(
                    patient,
                    doctor,
                    slot,
                    queueDate,
                    "Synthetic queue visit"));
            queueEntryRepository.saveAndFlush(new QueueEntry(
                    appointment,
                    doctor,
                    queueDate,
                    nextToken(),
                    tier,
                    Instant.now()));
        }

        List<QueueEntry> ordered = queueEntryRepository
                .findByDoctor_IdAndQueueDateAndStatusOrderByEmergencyTierAscArrivedAtAsc(
                        doctor.getId(),
                        queueDate,
                        QueueStatus.WAITING);

        assertThat(ordered).extracting(QueueEntry::getEmergencyTier).containsExactly(
                EmergencyTier.CRITICAL,
                EmergencyTier.HIGH,
                EmergencyTier.MEDIUM,
                EmergencyTier.NORMAL);
    }

    @Test
    void onlyOneQueueEntryCanBeInConsultationForADoctor() {
        DoctorProfile doctor = persistDoctor();
        PatientProfile patient = persistPatient();
        LocalDate queueDate = LocalDate.now();

        QueueEntry first = new QueueEntry(
                appointmentRepository.saveAndFlush(new Appointment(
                        patient,
                        doctor,
                        persistSlot(doctor),
                        queueDate,
                        "Synthetic first consultation")),
                doctor,
                queueDate,
                nextToken(),
                EmergencyTier.NORMAL,
                Instant.now());
        first.transitionTo(QueueStatus.IN_CONSULTATION);
        queueEntryRepository.saveAndFlush(first);

        QueueEntry second = new QueueEntry(
                appointmentRepository.saveAndFlush(new Appointment(
                        patient,
                        doctor,
                        persistSlot(doctor),
                        queueDate,
                        "Synthetic second consultation")),
                doctor,
                queueDate,
                nextToken(),
                EmergencyTier.NORMAL,
                Instant.now());
        second.transitionTo(QueueStatus.IN_CONSULTATION);

        assertThatThrownBy(() -> queueEntryRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void queueAuditRequiresANonBlankReason() {
        DoctorProfile doctor = persistDoctor();
        PatientProfile patient = persistPatient();
        Appointment appointment = appointmentRepository.saveAndFlush(new Appointment(
                patient,
                doctor,
                persistSlot(doctor),
                LocalDate.now(),
                "Synthetic audit visit"));
        QueueEntry queueEntry = queueEntryRepository.saveAndFlush(new QueueEntry(
                appointment,
                doctor,
                LocalDate.now(),
                nextToken(),
                EmergencyTier.NORMAL,
                Instant.now()));
        User actor = persistUser();

        assertThatThrownBy(() -> new QueueAudit(
                        queueEntry,
                        actor,
                        "PRIORITY_OVERRIDE",
                        "NORMAL",
                        "HIGH",
                        " "))
                .isInstanceOf(IllegalArgumentException.class);
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

    private Department persistDepartment() {
        String unique = UUID.randomUUID().toString();
        return departmentRepository.saveAndFlush(new Department("Synthetic-" + unique, "Synthetic department"));
    }

    private DoctorProfile persistDoctor() {
        User user = userRepository.saveAndFlush(new User(
                UserRole.DOCTOR,
                "Synthetic Doctor",
                "doctor-" + UUID.randomUUID() + "@example.test",
                "not-used-in-this-test",
                null));
        return doctorProfileRepository.saveAndFlush(new DoctorProfile(
                user,
                persistDepartment(),
                "General Medicine",
                "LICENSE-" + UUID.randomUUID(),
                15));
    }

    private ScheduleSlot persistSlot(DoctorProfile doctor) {
        Instant startsAt = Instant.parse("2030-01-01T00:00:00Z")
                .plusSeconds(SLOT_SEQUENCE.incrementAndGet() * 3600L);
        return scheduleSlotRepository.saveAndFlush(new ScheduleSlot(
                doctor,
                startsAt,
                startsAt.plusSeconds(900)));
    }

    private int nextToken() {
        return TOKEN_SEQUENCE.incrementAndGet();
    }
}
