package com.smarthospital.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smarthospital.appointment.Appointment;
import com.smarthospital.appointment.AppointmentRepository;
import com.smarthospital.department.Department;
import com.smarthospital.department.DepartmentRepository;
import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.doctor.DoctorProfileRepository;
import com.smarthospital.patient.PatientProfile;
import com.smarthospital.patient.PatientProfileRepository;
import com.smarthospital.queue.EmergencyTier;
import com.smarthospital.queue.QueueAudit;
import com.smarthospital.queue.QueueAuditRepository;
import com.smarthospital.queue.QueueEntry;
import com.smarthospital.queue.QueueEntryRepository;
import com.smarthospital.schedule.ScheduleSlot;
import com.smarthospital.schedule.ScheduleSlotRepository;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class QueueAuditRetentionTest extends MySqlIntegrationTest {

    private static final LocalDate TEST_DATE = LocalDate.of(2031, 5, 1);

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
    private QueueAuditRepository queueAuditRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deletingQueueEntryWithAuditHistoryIsRejectedAndAuditRemains() {
        DoctorProfile doctor = persistDoctor();
        PatientProfile patient = persistPatient();
        Appointment appointment = appointmentRepository.saveAndFlush(new Appointment(
                patient,
                doctor,
                persistSlot(doctor),
                TEST_DATE,
                "Synthetic audited visit"));
        QueueEntry queueEntry = queueEntryRepository.saveAndFlush(new QueueEntry(
                appointment,
                doctor,
                TEST_DATE,
                1,
                EmergencyTier.NORMAL,
                Instant.parse("2031-05-01T08:00:00Z")));
        QueueAudit audit = queueAuditRepository.saveAndFlush(new QueueAudit(
                queueEntry,
                persistUser(),
                "PRIORITY_OVERRIDE",
                "NORMAL",
                "HIGH",
                "Synthetic audited priority change"));

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "DELETE FROM queue_entries WHERE id = ?",
                        queueEntry.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("fk_queue_audits_entry");

        assertThat(queueAuditRepository.findById(audit.getId())).isPresent();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM queue_audits WHERE queue_entry_id = ?",
                Integer.class,
                queueEntry.getId())).isEqualTo(1);
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
        Instant startsAt = Instant.parse("2031-05-01T09:00:00Z");
        return scheduleSlotRepository.saveAndFlush(new ScheduleSlot(
                doctor,
                startsAt,
                startsAt.plusSeconds(900)));
    }
}
