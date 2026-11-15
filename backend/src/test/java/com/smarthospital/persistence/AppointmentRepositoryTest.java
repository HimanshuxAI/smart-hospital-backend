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
import com.smarthospital.schedule.ScheduleSlot;
import com.smarthospital.schedule.ScheduleSlotRepository;
import com.smarthospital.schedule.ScheduleSlotStatus;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class AppointmentRepositoryTest extends MySqlIntegrationTest {

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
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void patientCanBeLoadedWithAPessimisticWriteLock() {
        PatientProfile patient = persistPatient();

        PatientProfile locked = patientProfileRepository.findByIdForUpdate(patient.getId()).orElseThrow();

        assertThat(locked.getId()).isEqualTo(patient.getId());
        assertThat(locked.getUser().getId()).isEqualTo(patient.getUser().getId());
    }

    @Test
    void activeOverlapIncludesOnlyActiveAppointmentsForTheRequestedPatient() {
        DoctorProfile doctor = persistDoctor();
        PatientProfile patient = persistPatient();
        PatientProfile otherPatient = persistPatient();
        Instant rangeStart = Instant.parse("2031-04-01T10:00:00Z");
        Instant rangeEnd = Instant.parse("2031-04-01T11:00:00Z");

        ScheduleSlot bookedSlot = persistSlot(doctor, rangeStart, rangeStart.plusSeconds(1800));
        Appointment booked = appointmentRepository.saveAndFlush(new Appointment(
                patient,
                doctor,
                bookedSlot,
                LocalDate.of(2031, 4, 1),
                "Synthetic booked overlap"));

        ScheduleSlot checkedInSlot = persistSlot(doctor, rangeStart.plusSeconds(600), rangeStart.plusSeconds(2400));
        Appointment checkedIn = appointmentRepository.saveAndFlush(new Appointment(
                patient,
                doctor,
                checkedInSlot,
                LocalDate.of(2031, 4, 1),
                "Synthetic checked-in overlap"));
        checkedIn.checkIn(Instant.parse("2031-04-01T09:55:00Z"));
        appointmentRepository.saveAndFlush(checkedIn);

        ScheduleSlot terminalSlot = persistSlot(doctor, rangeStart.plusSeconds(7200), rangeStart.plusSeconds(9000));
        Appointment terminal = appointmentRepository.saveAndFlush(new Appointment(
                patient,
                doctor,
                terminalSlot,
                LocalDate.of(2031, 4, 1),
                "Synthetic terminal non-overlap"));
        terminal.markNoShow(Instant.parse("2031-04-01T09:55:00Z"));
        appointmentRepository.saveAndFlush(terminal);

        ScheduleSlot touchingStartSlot = persistSlot(
                doctor,
                rangeStart.minusSeconds(1800),
                rangeStart);
        appointmentRepository.saveAndFlush(new Appointment(
                patient,
                doctor,
                touchingStartSlot,
                LocalDate.of(2031, 4, 1),
                "Synthetic touching-start appointment"));

        ScheduleSlot touchingEndSlot = persistSlot(
                doctor,
                rangeEnd,
                rangeEnd.plusSeconds(1800));
        appointmentRepository.saveAndFlush(new Appointment(
                patient,
                doctor,
                touchingEndSlot,
                LocalDate.of(2031, 4, 1),
                "Synthetic touching-end appointment"));

        ScheduleSlot otherPatientSlot = persistSlot(doctor, rangeStart.plusSeconds(10800), rangeStart.plusSeconds(12600));
        appointmentRepository.saveAndFlush(new Appointment(
                otherPatient,
                doctor,
                otherPatientSlot,
                LocalDate.of(2031, 4, 1),
                "Synthetic other patient overlap"));

        List<Appointment> overlapping = appointmentRepository.findActiveOverlappingForPatient(
                patient.getId(),
                rangeStart,
                rangeEnd);

        assertThat(overlapping)
                .extracting(Appointment::getId)
                .containsExactlyInAnyOrder(booked.getId(), checkedIn.getId());
    }

    @Test
    void cancelledAppointmentAllowsTheSameSlotToBeRebookedWithoutLosingHistory() {
        DoctorProfile doctor = persistDoctor();
        PatientProfile patient = persistPatient();
        Instant startsAt = Instant.parse("2031-05-01T09:00:00Z");
        LocalDate appointmentDate = LocalDate.of(2031, 5, 1);
        ScheduleSlot slot = persistSlot(doctor, startsAt, startsAt.plusSeconds(1800));

        Appointment cancelledAppointment = appointmentRepository.saveAndFlush(new Appointment(
                patient,
                doctor,
                slot,
                appointmentDate,
                "Synthetic original booking"));
        slot.markBooked();
        scheduleSlotRepository.saveAndFlush(slot);

        cancelledAppointment.cancel(
                patient.getUser(),
                "Patient requested cancellation",
                startsAt.minusSeconds(3600));
        appointmentRepository.saveAndFlush(cancelledAppointment);

        slot.release();
        scheduleSlotRepository.saveAndFlush(slot);
        assertThat(slot.getStatus()).isEqualTo(ScheduleSlotStatus.OPEN);
        assertThat(slot.isActive()).isTrue();

        Appointment replacementAppointment = appointmentRepository.saveAndFlush(new Appointment(
                patient,
                doctor,
                slot,
                appointmentDate,
                "Synthetic replacement booking"));
        slot.markBooked();
        scheduleSlotRepository.saveAndFlush(slot);

        Appointment persistedCancelled = appointmentRepository
                .findById(cancelledAppointment.getId())
                .orElseThrow();
        Appointment persistedReplacement = appointmentRepository
                .findById(replacementAppointment.getId())
                .orElseThrow();
        ScheduleSlot persistedSlot = scheduleSlotRepository.findById(slot.getId()).orElseThrow();

        assertThat(persistedCancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(persistedCancelled.getActiveMarker()).isNull();
        assertThat(persistedReplacement.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
        assertThat(persistedReplacement.getActiveMarker()).isEqualTo(1);
        assertThat(persistedReplacement.getSlot().getId()).isEqualTo(slot.getId());
        assertThat(persistedSlot.getStatus()).isEqualTo(ScheduleSlotStatus.BOOKED);
        assertThat(persistedSlot.isActive()).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM appointments WHERE slot_id = ?",
                Integer.class,
                slot.getId())).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schedule_slots WHERE doctor_id = ?",
                Integer.class,
                doctor.getId())).isEqualTo(1);

        assertThatThrownBy(() -> appointmentRepository.saveAndFlush(new Appointment(
                        patient,
                        doctor,
                        slot,
                        appointmentDate,
                        "Synthetic conflicting replacement booking")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_appointments_active_slot");
    }

    private PatientProfile persistPatient() {
        String unique = UUID.randomUUID().toString();
        User user = userRepository.saveAndFlush(new User(
                UserRole.PATIENT,
                "Synthetic Patient",
                "patient-" + unique + "@example.test",
                "not-used-in-this-test",
                null));
        return patientProfileRepository.saveAndFlush(new PatientProfile(
                user,
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
}
