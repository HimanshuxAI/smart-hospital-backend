package com.smarthospital.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarthospital.appointment.Appointment;
import com.smarthospital.appointment.AppointmentRepository;
import com.smarthospital.department.Department;
import com.smarthospital.department.DepartmentRepository;
import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.doctor.DoctorProfileRepository;
import com.smarthospital.patient.PatientProfile;
import com.smarthospital.patient.PatientProfileRepository;
import com.smarthospital.persistence.MySqlIntegrationTest;
import com.smarthospital.queue.EmergencyTier;
import com.smarthospital.queue.QueueEntry;
import com.smarthospital.queue.QueueEntryRepository;
import com.smarthospital.schedule.ScheduleSlot;
import com.smarthospital.schedule.ScheduleSlotRepository;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class DashboardRepositoryTest extends MySqlIntegrationTest {

    private static final LocalDate TARGET_DATE = LocalDate.of(2042, 3, 17);
    private static final LocalDate OTHER_DATE = LocalDate.of(2042, 3, 16);

    @Autowired
    private DashboardService dashboardService;

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
    private EntityManager entityManager;

    @Autowired
    private SessionFactory sessionFactory;

    @TestBean(name = "clock", methodName = "fixedClock")
    private Clock clock;

    @Test
    @Transactional
    void dashboardQueriesAreScopedDateFilteredAndDoNotUseNPlus1() {
        User patientOne = persistUser(UserRole.PATIENT, "Patient One");
        User patientTwo = persistUser(UserRole.PATIENT, "Patient Two");
        User doctorOne = persistUser(UserRole.DOCTOR, "Doctor One");
        User doctorTwo = persistUser(UserRole.DOCTOR, "Doctor Two");
        persistUser(UserRole.ADMIN, "Admin One");

        PatientProfile patientOneProfile = patientProfileRepository.saveAndFlush(
                new PatientProfile(patientOne, LocalDate.of(1990, 1, 1)));
        PatientProfile patientTwoProfile = patientProfileRepository.saveAndFlush(
                new PatientProfile(patientTwo, LocalDate.of(1991, 1, 1)));
        Department department = departmentRepository.saveAndFlush(
                new Department("Dashboard-" + UUID.randomUUID(), "Dashboard department"));
        DoctorProfile doctorOneProfile = doctorProfileRepository.saveAndFlush(
                new DoctorProfile(
                        doctorOne,
                        department,
                        "General Medicine",
                        "DASH-" + UUID.randomUUID(),
                        15));
        DoctorProfile doctorTwoProfile = doctorProfileRepository.saveAndFlush(
                new DoctorProfile(
                        doctorTwo,
                        department,
                        "Cardiology",
                        "DASH-" + UUID.randomUUID(),
                        20));

        Appointment patientOneAppointment = persistAppointment(
                patientOneProfile,
                doctorOneProfile,
                TARGET_DATE,
                Instant.parse("2042-03-17T09:00:00Z"));
        Appointment patientTwoAppointment = persistAppointment(
                patientTwoProfile,
                doctorOneProfile,
                TARGET_DATE,
                Instant.parse("2042-03-17T10:00:00Z"));
        Appointment otherDoctorAppointment = persistAppointment(
                patientTwoProfile,
                doctorTwoProfile,
                TARGET_DATE,
                Instant.parse("2042-03-17T11:00:00Z"));
        persistAppointment(
                patientOneProfile,
                doctorOneProfile,
                OTHER_DATE,
                Instant.parse("2042-03-16T09:00:00Z"));

        QueueEntry patientOneQueue = persistQueue(
                patientOneAppointment,
                doctorOneProfile,
                TARGET_DATE,
                1);
        QueueEntry patientTwoQueue = persistQueue(
                patientTwoAppointment,
                doctorOneProfile,
                TARGET_DATE,
                2);
        persistQueue(
                otherDoctorAppointment,
                doctorTwoProfile,
                TARGET_DATE,
                1);

        entityManager.flush();
        entityManager.clear();
        sessionFactory.getStatistics().setStatisticsEnabled(true);

        sessionFactory.getStatistics().clear();
        DashboardService.PatientDashboardResponse patientDashboard =
                dashboardService.patientDashboard(patientOne.getId());
        assertThat(sessionFactory.getStatistics().getQueryExecutionCount()).isEqualTo(3);
        assertThat(patientDashboard.upcomingAppointments())
                .extracting(DashboardService.AppointmentView::appointmentId)
                .containsExactly(patientOneAppointment.getId());
        assertThat(patientDashboard.activeQueueEntry())
                .extracting(DashboardService.QueueView::entryId)
                .isEqualTo(patientOneQueue.getId());

        sessionFactory.getStatistics().clear();
        DashboardService.DoctorDashboardResponse doctorDashboard =
                dashboardService.doctorDashboard(doctorOne.getId());
        assertThat(sessionFactory.getStatistics().getQueryExecutionCount()).isEqualTo(3);
        assertThat(doctorDashboard.schedule())
                .extracting(DashboardService.AppointmentView::appointmentId)
                .containsExactly(
                        patientOneAppointment.getId(),
                        patientTwoAppointment.getId());
        assertThat(doctorDashboard.currentQueue())
                .extracting(DashboardService.QueueView::entryId)
                .containsExactly(
                        patientOneQueue.getId(),
                        patientTwoQueue.getId());

        sessionFactory.getStatistics().clear();
        DashboardService.AdminDashboardResponse adminDashboard =
                dashboardService.adminDashboard(TARGET_DATE);
        assertThat(sessionFactory.getStatistics().getQueryExecutionCount()).isEqualTo(5);
        assertThat(adminDashboard.date()).isEqualTo(TARGET_DATE);
        assertThat(adminDashboard.totals().appointmentTotal()).isEqualTo(3);
        assertThat(adminDashboard.totals().queueTotal()).isEqualTo(3);
    }

    static Clock fixedClock() {
        return Clock.fixed(Instant.parse("2042-03-17T12:00:00Z"), ZoneOffset.UTC);
    }

    private User persistUser(UserRole role, String fullName) {
        String unique = UUID.randomUUID().toString();
        return userRepository.saveAndFlush(new User(
                role,
                fullName,
                role.name().toLowerCase() + "-" + unique + "@example.test",
                "not-used-in-this-test",
                null));
    }

    private Appointment persistAppointment(
            PatientProfile patient,
            DoctorProfile doctor,
            LocalDate date,
            Instant startsAt) {
        ScheduleSlot slot = scheduleSlotRepository.saveAndFlush(new ScheduleSlot(
                doctor,
                startsAt,
                startsAt.plusSeconds(1_200)));
        return appointmentRepository.saveAndFlush(new Appointment(
                patient,
                doctor,
                slot,
                date,
                "Synthetic dashboard appointment"));
    }

    private QueueEntry persistQueue(
            Appointment appointment,
            DoctorProfile doctor,
            LocalDate date,
            int tokenNumber) {
        return queueEntryRepository.saveAndFlush(new QueueEntry(
                appointment,
                doctor,
                date,
                tokenNumber,
                EmergencyTier.NORMAL,
                appointment.getSlot().getStartsAt().minusSeconds(600)));
    }
}
