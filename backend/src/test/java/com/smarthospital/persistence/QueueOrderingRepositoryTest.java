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
import com.smarthospital.queue.EmergencyTier;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class QueueOrderingRepositoryTest extends MySqlIntegrationTest {

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

    @Test
    void canonicalOrderingUsesPriorityThenArrivalThenId() {
        DoctorProfile doctor = persistDoctor();
        PatientProfile patient = persistPatient();
        LocalDate queueDate = LocalDate.of(2031, 6, 1);
        Instant normalArrival = Instant.parse("2031-06-01T08:00:00Z");
        Instant laterNormalArrival = normalArrival.plusSeconds(60);

        QueueEntry normal = persistEntry(doctor, patient, queueDate, EmergencyTier.NORMAL, normalArrival);
        QueueEntry normalTie = persistEntry(doctor, patient, queueDate, EmergencyTier.NORMAL, normalArrival);
        QueueEntry critical = persistEntry(doctor, patient, queueDate, EmergencyTier.CRITICAL, normalArrival);
        QueueEntry medium = persistEntry(doctor, patient, queueDate, EmergencyTier.MEDIUM, normalArrival);
        QueueEntry high = persistEntry(doctor, patient, queueDate, EmergencyTier.HIGH, normalArrival);
        QueueEntry laterNormal = persistEntry(doctor, patient, queueDate, EmergencyTier.NORMAL, laterNormalArrival);

        assertThat(normal.getId()).isLessThan(normalTie.getId());

        List<QueueEntry> ordered = queueEntryRepository.findCanonicalQueueForDoctorAndStatus(
                doctor.getId(),
                queueDate,
                QueueStatus.WAITING);

        assertThat(ordered)
                .extracting(QueueEntry::getId)
                .containsExactly(
                        critical.getId(),
                        high.getId(),
                        medium.getId(),
                        normal.getId(),
                        normalTie.getId(),
                        laterNormal.getId());
    }

    @Test
    @Transactional
    void lockedCandidateQueryUsesTheSameCanonicalOrdering() {
        DoctorProfile doctor = persistDoctor();
        PatientProfile patient = persistPatient();
        LocalDate queueDate = LocalDate.of(2031, 6, 1);
        Instant arrival = Instant.parse("2031-06-01T08:00:00Z");

        QueueEntry normal = persistEntry(doctor, patient, queueDate, EmergencyTier.NORMAL, arrival);
        QueueEntry critical = persistEntry(doctor, patient, queueDate, EmergencyTier.CRITICAL, arrival);
        QueueEntry high = persistEntry(doctor, patient, queueDate, EmergencyTier.HIGH, arrival);

        List<QueueEntry> lockedCandidates = queueEntryRepository.findNextCandidatesForUpdate(
                doctor.getId(),
                queueDate,
                List.of(QueueStatus.WAITING));

        assertThat(lockedCandidates)
                .extracting(QueueEntry::getId)
                .containsExactly(critical.getId(), high.getId(), normal.getId());
    }

    private QueueEntry persistEntry(
            DoctorProfile doctor,
            PatientProfile patient,
            LocalDate queueDate,
            EmergencyTier tier,
            Instant arrivedAt) {
        Appointment appointment = appointmentRepository.saveAndFlush(new Appointment(
                patient,
                doctor,
                persistSlot(doctor),
                queueDate,
                "Synthetic queue ordering visit"));
        return queueEntryRepository.saveAndFlush(new QueueEntry(
                appointment,
                doctor,
                queueDate,
                TOKEN_SEQUENCE.incrementAndGet(),
                tier,
                arrivedAt));
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

    private ScheduleSlot persistSlot(DoctorProfile doctor) {
        Instant startsAt = Instant.parse("2031-06-01T09:00:00Z")
                .plusSeconds(TOKEN_SEQUENCE.incrementAndGet() * 900L);
        return scheduleSlotRepository.saveAndFlush(new ScheduleSlot(
                doctor,
                startsAt,
                startsAt.plusSeconds(900)));
    }
}
