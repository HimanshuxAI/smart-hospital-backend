package com.smarthospital.dashboard;

import com.smarthospital.appointment.AppointmentRepository;
import com.smarthospital.appointment.AppointmentStatus;
import com.smarthospital.common.ApiException;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final AppointmentRepository appointmentRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final Clock clock;
    private final ZoneId hospitalZoneId;

    public DashboardService(
            PatientProfileRepository patientProfileRepository,
            DoctorProfileRepository doctorProfileRepository,
            AppointmentRepository appointmentRepository,
            QueueEntryRepository queueEntryRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            Clock clock,
            ZoneId hospitalZoneId) {
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.appointmentRepository = appointmentRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.clock = clock;
        this.hospitalZoneId = hospitalZoneId;
    }

    public PatientDashboardResponse patientDashboard(Long authenticatedUserId) {
        LocalDate date = currentDate();
        PatientProfile profile = patientProfileRepository.findByUserId(authenticatedUserId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "PATIENT_PROFILE_NOT_FOUND",
                        "Patient profile was not found"));

        List<AppointmentView> upcomingAppointments = appointmentRepository
                .findUpcomingForPatient(profile.getId(), date)
                .stream()
                .map(this::toAppointmentView)
                .toList();
        QueueView activeQueueEntry = queueEntryRepository
                .findFirstActiveForPatient(profile.getId(), date, Pageable.ofSize(1))
                .stream()
                .findFirst()
                .map(this::toQueueView)
                .orElse(null);
        return new PatientDashboardResponse(date, upcomingAppointments, activeQueueEntry);
    }

    public DoctorDashboardResponse doctorDashboard(Long authenticatedUserId) {
        LocalDate date = currentDate();
        DoctorProfile profile = doctorProfileRepository.findByUserId(authenticatedUserId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "DOCTOR_PROFILE_NOT_FOUND",
                        "Doctor profile was not found"));

        List<AppointmentView> schedule = appointmentRepository
                .findScheduleForDoctor(profile.getId(), date)
                .stream()
                .map(this::toAppointmentView)
                .toList();
        List<QueueView> currentQueue = queueEntryRepository
                .findCurrentQueueForDoctor(profile.getId(), date)
                .stream()
                .map(this::toQueueView)
                .toList();
        return new DoctorDashboardResponse(date, schedule, currentQueue);
    }

    public AdminDashboardResponse adminDashboard(LocalDate selectedDate) {
        LocalDate date = selectedDate == null ? currentDate() : selectedDate;
        AdminTotals totals = new AdminTotals(
                userRepository.count(),
                departmentRepository.count(),
                doctorProfileRepository.count(),
                appointmentRepository.countByAppointmentDate(date),
                queueEntryRepository.countByQueueDate(date));
        return new AdminDashboardResponse(date, totals);
    }

    private LocalDate currentDate() {
        return LocalDate.now(clock.withZone(hospitalZoneId));
    }

    private AppointmentView toAppointmentView(DashboardAppointmentProjection projection) {
        return new AppointmentView(
                projection.getAppointmentId(),
                projection.getDoctorName(),
                projection.getSpecialty(),
                projection.getPatientName(),
                projection.getStartsAt(),
                projection.getEndsAt(),
                projection.getStatus(),
                projection.getVisitReason());
    }

    private QueueView toQueueView(DashboardQueueProjection projection) {
        return new QueueView(
                projection.getEntryId(),
                projection.getAppointmentId(),
                projection.getDoctorName(),
                projection.getPatientName(),
                projection.getQueueDate(),
                projection.getTokenNumber(),
                projection.getEmergencyTier(),
                projection.getStatus(),
                projection.getArrivedAt(),
                projection.getScheduledStart());
    }

    public record PatientDashboardResponse(
            LocalDate date,
            List<AppointmentView> upcomingAppointments,
            QueueView activeQueueEntry) {
    }

    public record DoctorDashboardResponse(
            LocalDate date,
            List<AppointmentView> schedule,
            List<QueueView> currentQueue) {
    }

    public record AdminDashboardResponse(
            LocalDate date,
            AdminTotals totals) {
    }

    public record AppointmentView(
            Long appointmentId,
            String doctorName,
            String specialty,
            String patientName,
            Instant startsAt,
            Instant endsAt,
            AppointmentStatus status,
            String visitReason) {
    }

    public record QueueView(
            Long entryId,
            Long appointmentId,
            String doctorName,
            String patientName,
            LocalDate queueDate,
            Integer tokenNumber,
            EmergencyTier emergencyTier,
            QueueStatus status,
            Instant arrivedAt,
            Instant scheduledStart) {
    }

    public record AdminTotals(
            long userTotal,
            long departmentTotal,
            long doctorTotal,
            long appointmentTotal,
            long queueTotal) {
    }
}
