package com.smarthospital.appointment;

import com.smarthospital.common.ApiException;
import com.smarthospital.config.ClinicalCheckInProperties;
import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.doctor.DoctorProfileRepository;
import com.smarthospital.patient.PatientProfile;
import com.smarthospital.patient.PatientProfileRepository;
import com.smarthospital.queue.EmergencyTier;
import com.smarthospital.queue.QueueCheckInResponse;
import com.smarthospital.queue.QueueEntry;
import com.smarthospital.queue.QueueEntryRepository;
import com.smarthospital.queue.QueueStatus;
import com.smarthospital.queue.TokenService;
import com.smarthospital.schedule.ScheduleSlot;
import com.smarthospital.schedule.ScheduleSlotRepository;
import com.smarthospital.schedule.ScheduleSlotStatus;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final ClinicalCheckInProperties checkInProperties;
    private final Clock clock;
    private final ZoneId hospitalZoneId;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            ScheduleSlotRepository scheduleSlotRepository,
            PatientProfileRepository patientProfileRepository,
            DoctorProfileRepository doctorProfileRepository,
            QueueEntryRepository queueEntryRepository,
            TokenService tokenService,
            UserRepository userRepository,
            ClinicalCheckInProperties checkInProperties,
            Clock clock,
            ZoneId hospitalZoneId) {
        this.appointmentRepository = appointmentRepository;
        this.scheduleSlotRepository = scheduleSlotRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.checkInProperties = checkInProperties;
        this.clock = clock;
        this.hospitalZoneId = hospitalZoneId;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentResponse book(Long actorUserId, BookAppointmentRequest request) {
        User actor = findUser(actorUserId);
        if (actor.getRole() != UserRole.PATIENT) {
            throw forbidden();
        }
        PatientProfile patient = patientProfileRepository.findByUserId(actorUserId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "PATIENT_PROFILE_NOT_FOUND",
                        "Patient profile was not found"));

        // Booking lock order: slot -> patient. The slot lock serializes attempts
        // to consume one slot; the patient lock serializes overlapping bookings
        // for one patient across different slots.
        ScheduleSlot slot = scheduleSlotRepository.findByIdForUpdate(request.slotId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "SLOT_NOT_FOUND",
                        "Schedule slot was not found"));
        if (slot.getStatus() == ScheduleSlotStatus.BOOKED) {
            throw slotAlreadyBooked();
        }
        if (slot.getStatus() != ScheduleSlotStatus.OPEN || !slot.isActive()) {
            throw slotUnavailable();
        }
        Instant now = Instant.now(clock);
        if (!slot.getStartsAt().isAfter(now)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "SLOT_NOT_FUTURE",
                    "Schedule slot start is not in the future");
        }
        DoctorProfile doctor = slot.getDoctor();
        if (request.doctorId() != null && !request.doctorId().equals(doctor.getId())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "DOCTOR_SLOT_MISMATCH",
                    "Doctor does not own the requested schedule slot");
        }
        if (!doctor.isActive()
                || !doctor.getUser().isActive()
                || !doctor.getDepartment().isActive()) {
            throw slotUnavailable();
        }

        PatientProfile lockedPatient = patientProfileRepository
                .findByIdForUpdate(patient.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "PATIENT_PROFILE_NOT_FOUND",
                        "Patient profile was not found"));
        if (!appointmentRepository.findActiveOverlappingForPatient(
                        lockedPatient.getId(),
                        slot.getStartsAt(),
                        slot.getEndsAt())
                .isEmpty()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PATIENT_APPOINTMENT_OVERLAP",
                    "Patient already has an overlapping active appointment");
        }

        LocalDate appointmentDate = slot.getStartsAt()
                .atZone(hospitalZoneId)
                .toLocalDate();
        Appointment appointment = new Appointment(
                lockedPatient,
                doctor,
                slot,
                appointmentDate,
                request.visitReason().trim());
        try {
            appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException exception) {
            throw slotAlreadyBooked();
        }
        slot.markBooked();
        scheduleSlotRepository.saveAndFlush(slot);
        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse cancel(
            Long actorUserId,
            Long appointmentId,
            String reason) {
        User actor = findUser(actorUserId);
        Long doctorId = appointmentRepository.findDoctorIdById(appointmentId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "APPOINTMENT_NOT_FOUND",
                        "Appointment was not found"));

        // Clinical workflow lock order: doctor -> appointment -> queue -> slot.
        // The doctor row is the per-doctor mutex used by check-in and queue work.
        doctorProfileRepository.findByIdForUpdate(doctorId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "DOCTOR_NOT_FOUND",
                        "Doctor was not found"));
        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "APPOINTMENT_NOT_FOUND",
                        "Appointment was not found"));
        verifyCancellationAccess(actor, appointment);
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "APPOINTMENT_NOT_CANCELLABLE",
                    "Appointment cannot be cancelled in its current status");
        }

        ScheduleSlot slot = scheduleSlotRepository.findByIdForUpdate(appointment.getSlot().getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "SLOT_NOT_FOUND",
                        "Schedule slot was not found"));
        if (slot.getStatus() != ScheduleSlotStatus.BOOKED || !slot.isActive()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "SLOT_NOT_RELEASABLE",
                    "Booked schedule slot cannot be released");
        }

        appointment.cancel(actor, reason.trim(), Instant.now(clock));
        slot.release();
        appointmentRepository.saveAndFlush(appointment);
        scheduleSlotRepository.saveAndFlush(slot);
        return toResponse(appointment);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QueueCheckInResponse checkIn(Long actorUserId, Long appointmentId) {
        User actor = findUser(actorUserId);
        Long doctorId = appointmentRepository.findDoctorIdById(appointmentId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "APPOINTMENT_NOT_FOUND",
                        "Appointment was not found"));
        LocalDate appointmentDate = appointmentRepository.findAppointmentDateById(appointmentId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "APPOINTMENT_NOT_FOUND",
                        "Appointment was not found"));

        // Reserve the daily token while locking the doctor row, then lock the
        // appointment and any queue entry. All clinical state transitions for
        // one doctor therefore use doctor -> appointment -> queue ordering.
        TokenService.TokenAllocation allocation = tokenService.reserve(
                doctorId,
                appointmentDate);
        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "APPOINTMENT_NOT_FOUND",
                        "Appointment was not found"));
        verifyCheckInAccess(actor, appointment);

        if (appointment.getStatus() == AppointmentStatus.CHECKED_IN) {
            throw alreadyCheckedIn();
        }
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "APPOINTMENT_NOT_CHECKIN_ELIGIBLE",
                    "Appointment cannot be checked in in its current status");
        }

        Instant now = Instant.now(clock);
        LocalDate today = now.atZone(hospitalZoneId).toLocalDate();
        if (!today.equals(appointment.getAppointmentDate())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "APPOINTMENT_DATE_MISMATCH",
                    "Appointment date does not match the current queue date");
        }
        Instant scheduledStart = appointment.getSlot().getStartsAt();
        Instant earliest = scheduledStart.minus(
                checkInProperties.getEarlyMinutes(),
                ChronoUnit.MINUTES);
        Instant latest = scheduledStart.plus(
                checkInProperties.getLateMinutes(),
                ChronoUnit.MINUTES);
        if (now.isBefore(earliest)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CHECK_IN_TOO_EARLY",
                    "Appointment check-in window has not opened");
        }
        if (now.isAfter(latest)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "CHECK_IN_TOO_LATE",
                    "Appointment check-in window has closed");
        }
        if (queueEntryRepository.findByAppointment_IdForUpdate(appointmentId).isPresent()) {
            throw alreadyCheckedIn();
        }

        appointment.checkIn(now);
        appointmentRepository.saveAndFlush(appointment);
        QueueEntry entry = tokenService.createEntry(
                appointment,
                allocation.doctor(),
                appointment.getAppointmentDate(),
                allocation.tokenNumber(),
                now);
        long position = queueEntryRepository.countWaitingEntriesAhead(
                allocation.doctor().getId(),
                appointment.getAppointmentDate(),
                QueueStatus.WAITING,
                EmergencyTier.NORMAL,
                entry.getArrivedAt(),
                entry.getId());
        return new QueueCheckInResponse(
                entry.getId(),
                entry.getTokenNumber(),
                entry.getTokenNumber(),
                entry.getStatus(),
                entry.getEmergencyTier(),
                Math.toIntExact(position));
    }

    @Transactional(readOnly = true)
    public AppointmentResponse get(Long actorUserId, Long appointmentId) {
        User actor = findUser(actorUserId);
        Appointment appointment = findAppointment(appointmentId);
        verifyReadAccess(actor, appointment);
        return toResponse(appointment);
    }

    private Appointment findAppointment(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "APPOINTMENT_NOT_FOUND",
                        "Appointment was not found"));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "User was not found"));
    }

    private void verifyCancellationAccess(User actor, Appointment appointment) {
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (actor.getRole() == UserRole.PATIENT
                && appointment.getPatient().getUser().getId().equals(actor.getId())) {
            return;
        }
        throw forbidden();
    }

    private void verifyCheckInAccess(User actor, Appointment appointment) {
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (actor.getRole() == UserRole.PATIENT
                && appointment.getPatient().getUser().getId().equals(actor.getId())) {
            return;
        }
        if (actor.getRole() == UserRole.DOCTOR) {
            DoctorProfile actorDoctor = doctorProfileRepository.findByUserId(actor.getId())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "DOCTOR_PROFILE_NOT_FOUND",
                            "Doctor profile was not found"));
            if (actorDoctor.getId().equals(appointment.getDoctor().getId())) {
                return;
            }
        }
        throw forbidden();
    }

    private void verifyReadAccess(User actor, Appointment appointment) {
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (actor.getRole() == UserRole.PATIENT
                && appointment.getPatient().getUser().getId().equals(actor.getId())) {
            return;
        }
        if (actor.getRole() == UserRole.DOCTOR
                && appointment.getDoctor().getUser().getId().equals(actor.getId())) {
            return;
        }
        throw forbidden();
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatient().getId(),
                appointment.getDoctor().getId(),
                appointment.getSlot().getId(),
                appointment.getAppointmentDate(),
                appointment.getSlot().getStartsAt(),
                appointment.getSlot().getEndsAt(),
                appointment.getVisitReason(),
                appointment.getStatus(),
                appointment.getCheckedInAt(),
                appointment.getCompletedAt(),
                appointment.getCancelledAt(),
                appointment.getCancellationReason());
    }

    private static ApiException slotAlreadyBooked() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "SLOT_ALREADY_BOOKED",
                "Schedule slot has already been booked");
    }

    private static ApiException slotUnavailable() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "SLOT_UNAVAILABLE",
                "Schedule slot is not available for booking");
    }

    private static ApiException alreadyCheckedIn() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "ALREADY_CHECKED_IN",
                "Appointment has already been checked in");
    }

    private static ApiException forbidden() {
        return new ApiException(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "Access is denied");
    }
}
