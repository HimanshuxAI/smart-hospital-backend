package com.smarthospital.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.smarthospital.appointment.AppointmentRepository;
import com.smarthospital.appointment.AppointmentStatus;
import com.smarthospital.department.DepartmentRepository;
import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.doctor.DoctorProfileRepository;
import com.smarthospital.patient.PatientProfile;
import com.smarthospital.patient.PatientProfileRepository;
import com.smarthospital.queue.EmergencyTier;
import com.smarthospital.queue.QueueEntryRepository;
import com.smarthospital.queue.QueueStatus;
import com.smarthospital.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final ZoneId HOSPITAL_ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalDate LOCAL_DATE = LocalDate.of(2030, 1, 16);

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(
                patientProfileRepository,
                doctorProfileRepository,
                appointmentRepository,
                queueEntryRepository,
                userRepository,
                departmentRepository,
                Clock.fixed(Instant.parse("2030-01-15T23:30:00Z"), HOSPITAL_ZONE),
                HOSPITAL_ZONE);
    }

    @Test
    void patientDashboardUsesAuthenticatedPatientProfileAndOnlyOwnRows() {
        PatientProfile profile = mock(PatientProfile.class);
        when(profile.getId()).thenReturn(11L);
        when(patientProfileRepository.findByUserId(44L)).thenReturn(Optional.of(profile));

        DashboardAppointmentProjection appointment = mock(DashboardAppointmentProjection.class);
        when(appointment.getAppointmentId()).thenReturn(101L);
        when(appointment.getDoctorName()).thenReturn("Dr Patient");
        when(appointment.getSpecialty()).thenReturn("General Medicine");
        when(appointment.getStartsAt()).thenReturn(Instant.parse("2030-01-16T09:00:00Z"));
        when(appointment.getEndsAt()).thenReturn(Instant.parse("2030-01-16T09:20:00Z"));
        when(appointment.getStatus()).thenReturn(AppointmentStatus.BOOKED);
        when(appointment.getVisitReason()).thenReturn("Check-up");
        when(appointmentRepository.findUpcomingForPatient(11L, LOCAL_DATE))
                .thenReturn(List.of(appointment));

        DashboardQueueProjection queue = mock(DashboardQueueProjection.class);
        when(queue.getEntryId()).thenReturn(201L);
        when(queue.getAppointmentId()).thenReturn(101L);
        when(queue.getDoctorName()).thenReturn("Dr Patient");
        when(queue.getPatientName()).thenReturn("Patient One");
        when(queue.getQueueDate()).thenReturn(LOCAL_DATE);
        when(queue.getTokenNumber()).thenReturn(3);
        when(queue.getEmergencyTier()).thenReturn(EmergencyTier.NORMAL);
        when(queue.getStatus()).thenReturn(QueueStatus.WAITING);
        when(queue.getArrivedAt()).thenReturn(Instant.parse("2030-01-16T08:50:00Z"));
        when(queue.getScheduledStart()).thenReturn(Instant.parse("2030-01-16T09:00:00Z"));
        when(queueEntryRepository.findFirstActiveForPatient(
                eq(11L), eq(LOCAL_DATE), any(Pageable.class)))
                .thenReturn(List.of(queue));

        DashboardService.PatientDashboardResponse response =
                service.patientDashboard(44L);

        assertThat(response.date()).isEqualTo(LOCAL_DATE);
        assertThat(response.upcomingAppointments())
                .extracting(DashboardService.AppointmentView::appointmentId)
                .containsExactly(101L);
        assertThat(response.activeQueueEntry()).isNotNull();
        assertThat(response.activeQueueEntry().entryId()).isEqualTo(201L);
        verify(patientProfileRepository).findByUserId(44L);
        verifyNoInteractions(doctorProfileRepository);
    }

    @Test
    void doctorDashboardDerivesDoctorProfileFromAuthenticatedUser() {
        DoctorProfile profile = mock(DoctorProfile.class);
        when(profile.getId()).thenReturn(21L);
        when(doctorProfileRepository.findByUserId(55L)).thenReturn(Optional.of(profile));

        DashboardAppointmentProjection appointment = mock(DashboardAppointmentProjection.class);
        when(appointment.getAppointmentId()).thenReturn(102L);
        when(appointment.getPatientName()).thenReturn("Patient One");
        when(appointment.getDoctorName()).thenReturn("Dr Doctor");
        when(appointment.getStartsAt()).thenReturn(Instant.parse("2030-01-16T10:00:00Z"));
        when(appointment.getEndsAt()).thenReturn(Instant.parse("2030-01-16T10:20:00Z"));
        when(appointment.getStatus()).thenReturn(AppointmentStatus.BOOKED);
        when(appointment.getVisitReason()).thenReturn("Review");
        when(appointmentRepository.findScheduleForDoctor(21L, LOCAL_DATE))
                .thenReturn(List.of(appointment));

        DashboardQueueProjection queue = mock(DashboardQueueProjection.class);
        when(queue.getEntryId()).thenReturn(202L);
        when(queue.getAppointmentId()).thenReturn(102L);
        when(queue.getDoctorName()).thenReturn("Dr Doctor");
        when(queue.getPatientName()).thenReturn("Patient One");
        when(queue.getQueueDate()).thenReturn(LOCAL_DATE);
        when(queue.getTokenNumber()).thenReturn(1);
        when(queue.getEmergencyTier()).thenReturn(EmergencyTier.HIGH);
        when(queue.getStatus()).thenReturn(QueueStatus.WAITING);
        when(queue.getArrivedAt()).thenReturn(Instant.parse("2030-01-16T09:50:00Z"));
        when(queue.getScheduledStart()).thenReturn(Instant.parse("2030-01-16T10:00:00Z"));
        when(queueEntryRepository.findCurrentQueueForDoctor(21L, LOCAL_DATE))
                .thenReturn(List.of(queue));

        DashboardService.DoctorDashboardResponse response =
                service.doctorDashboard(55L);

        assertThat(response.date()).isEqualTo(LOCAL_DATE);
        assertThat(response.schedule())
                .extracting(DashboardService.AppointmentView::appointmentId)
                .containsExactly(102L);
        assertThat(response.currentQueue())
                .extracting(DashboardService.QueueView::entryId)
                .containsExactly(202L);
        verify(doctorProfileRepository).findByUserId(55L);
        verifyNoInteractions(patientProfileRepository);
    }

    @Test
    void adminDashboardUsesSelectedDateForAppointmentAndQueueTotals() {
        when(userRepository.count()).thenReturn(20L);
        when(departmentRepository.count()).thenReturn(5L);
        when(doctorProfileRepository.count()).thenReturn(8L);
        when(appointmentRepository.countByAppointmentDate(LOCAL_DATE)).thenReturn(12L);
        when(queueEntryRepository.countByQueueDate(LOCAL_DATE)).thenReturn(9L);

        DashboardService.AdminDashboardResponse response =
                service.adminDashboard(LOCAL_DATE);

        assertThat(response.date()).isEqualTo(LOCAL_DATE);
        assertThat(response.totals().userTotal()).isEqualTo(20L);
        assertThat(response.totals().departmentTotal()).isEqualTo(5L);
        assertThat(response.totals().doctorTotal()).isEqualTo(8L);
        assertThat(response.totals().appointmentTotal()).isEqualTo(12L);
        assertThat(response.totals().queueTotal()).isEqualTo(9L);
    }

    @Test
    void adminDashboardDefaultsToTodayInConfiguredHospitalZone() {
        when(userRepository.count()).thenReturn(1L);
        when(departmentRepository.count()).thenReturn(1L);
        when(doctorProfileRepository.count()).thenReturn(1L);
        when(appointmentRepository.countByAppointmentDate(LOCAL_DATE)).thenReturn(2L);
        when(queueEntryRepository.countByQueueDate(LOCAL_DATE)).thenReturn(3L);

        DashboardService.AdminDashboardResponse response = service.adminDashboard(null);

        assertThat(response.date()).isEqualTo(LOCAL_DATE);
        verify(appointmentRepository).countByAppointmentDate(LOCAL_DATE);
        verify(queueEntryRepository).countByQueueDate(LOCAL_DATE);
    }
}
