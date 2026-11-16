package com.smarthospital.queue;

import com.smarthospital.appointment.Appointment;
import com.smarthospital.appointment.AppointmentRepository;
import com.smarthospital.appointment.AppointmentStatus;
import com.smarthospital.common.ApiException;
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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueueService {

    private static final Set<QueueStatus> ACTIVE_FLOW_STATUSES = Set.of(
            QueueStatus.CALLED,
            QueueStatus.IN_CONSULTATION);

    private final QueueEntryRepository queueEntryRepository;
    private final QueueAuditRepository queueAuditRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final ZoneId hospitalZoneId;

    public QueueService(
            QueueEntryRepository queueEntryRepository,
            QueueAuditRepository queueAuditRepository,
            AppointmentRepository appointmentRepository,
            DoctorProfileRepository doctorProfileRepository,
            PatientProfileRepository patientProfileRepository,
            ScheduleSlotRepository scheduleSlotRepository,
            UserRepository userRepository,
            Clock clock,
            ZoneId hospitalZoneId) {
        this.queueEntryRepository = queueEntryRepository;
        this.queueAuditRepository = queueAuditRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.scheduleSlotRepository = scheduleSlotRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.hospitalZoneId = hospitalZoneId;
    }

    @Transactional(readOnly = true)
    public List<QueueEntryResponse> list(
            Long actorUserId,
            Long requestedDoctorId,
            LocalDate requestedDate) {
        User actor = findUser(actorUserId);
        LocalDate queueDate = resolveQueueDate(requestedDate);
        if (actor.getRole() == UserRole.PATIENT) {
            if (requestedDoctorId != null) {
                throw forbidden();
            }
            return listPatientQueue(actor, queueDate);
        }
        if (actor.getRole() == UserRole.DOCTOR) {
            DoctorProfile actorDoctor = findDoctorForUser(actor);
            if (requestedDoctorId != null
                    && !requestedDoctorId.equals(actorDoctor.getId())) {
                throw forbidden();
            }
            return listDoctorQueue(actorDoctor.getId(), queueDate);
        }
        if (actor.getRole() == UserRole.ADMIN) {
            if (requestedDoctorId == null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "DOCTOR_SCOPE_REQUIRED",
                        "A doctorId is required for an administrator queue query");
            }
            if (!doctorProfileRepository.existsById(requestedDoctorId)) {
                throw new ApiException(
                        HttpStatus.NOT_FOUND,
                        "DOCTOR_NOT_FOUND",
                        "Doctor was not found");
            }
            return listDoctorQueue(requestedDoctorId, queueDate);
        }
        throw forbidden();
    }

    @Transactional(readOnly = true)
    public QueueEntryResponse get(Long actorUserId, Long entryId) {
        User actor = findUser(actorUserId);
        QueueEntry entry = queueEntryRepository.findDetailedById(entryId)
                .orElseThrow(() -> queueEntryNotFound());
        verifyReadAccess(actor, entry);
        int position = positionOf(entry);
        return toResponse(entry, position);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QueueEntryResponse classify(
            Long actorUserId,
            Long entryId,
            ClassifyQueueEntryRequest request) {
        return classifyPriority(actorUserId, entryId, request);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QueueEntryResponse override(
            Long actorUserId,
            Long entryId,
            ClassifyQueueEntryRequest request) {
        return classifyPriority(actorUserId, entryId, request);
    }

    private QueueEntryResponse classifyPriority(
            Long actorUserId,
            Long entryId,
            ClassifyQueueEntryRequest request) {
        User actor = findUser(actorUserId);
        if (request == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Request validation failed");
        }
        String reason = requireReason(request.reason());
        EmergencyTier newTier = requireTier(request.triageLevel());
        LockedQueue lockedQueue = lockForStaff(actor, entryId);
        QueueEntry entry = lockedQueue.entry();
        requireStatus(entry, QueueStatus.WAITING, "classify");

        EmergencyTier previousTier = entry.getEmergencyTier();
        entry.classify(newTier);
        queueEntryRepository.saveAndFlush(entry);
        queueAuditRepository.saveAndFlush(new QueueAudit(
                entry,
                actor,
                "PRIORITY_OVERRIDE",
                previousTier.name(),
                newTier.name(),
                reason));
        return toResponse(entry, positionOf(entry));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QueueEntryResponse call(Long actorUserId, Long entryId) {
        User actor = findUser(actorUserId);
        LockedQueue lockedQueue = lockForStaff(actor, entryId);
        QueueEntry entry = lockedQueue.entry();
        requireStatus(entry, QueueStatus.WAITING, "call");

        long activeEntries = queueEntryRepository.countByDoctor_IdAndQueueDateAndStatusIn(
                lockedQueue.doctor().getId(),
                entry.getQueueDate(),
                ACTIVE_FLOW_STATUSES);
        if (activeEntries > 0) {
            throw invalidTransition("Another queue entry is already active for this doctor");
        }

        List<QueueEntry> waitingEntries = queueEntryRepository.findCanonicalQueueForDoctorAndStatus(
                lockedQueue.doctor().getId(),
                entry.getQueueDate(),
                QueueStatus.WAITING);
        if (waitingEntries.isEmpty()
                || !waitingEntries.get(0).getId().equals(entry.getId())) {
            throw invalidTransition("Only the canonical next queue entry can be called");
        }

        entry.call(Instant.now(clock));
        queueEntryRepository.saveAndFlush(entry);
        return toResponse(entry, 0);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QueueEntryResponse serve(Long actorUserId, Long entryId) {
        User actor = findUser(actorUserId);
        LockedQueue lockedQueue = lockForStaff(actor, entryId);
        QueueEntry entry = lockedQueue.entry();
        requireStatus(entry, QueueStatus.CALLED, "serve");

        long activeConsultations = queueEntryRepository.countByDoctor_IdAndQueueDateAndStatus(
                lockedQueue.doctor().getId(),
                entry.getQueueDate(),
                QueueStatus.IN_CONSULTATION);
        if (activeConsultations > 0) {
            throw invalidTransition("This doctor already has an entry in consultation");
        }

        entry.startConsultation(Instant.now(clock));
        queueEntryRepository.saveAndFlush(entry);
        return toResponse(entry, 0);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QueueEntryResponse complete(Long actorUserId, Long entryId) {
        User actor = findUser(actorUserId);
        LockedQueue lockedQueue = lockForStaff(actor, entryId);
        QueueEntry entry = lockedQueue.entry();
        Appointment appointment = lockedQueue.appointment();
        requireStatus(entry, QueueStatus.IN_CONSULTATION, "complete");
        if (appointment.getStatus() != AppointmentStatus.CHECKED_IN) {
            throw invalidTransition("The appointment is not checked in");
        }

        ScheduleSlot slot = scheduleSlotRepository.findByIdForUpdate(
                        appointment.getSlot().getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "SLOT_NOT_FOUND",
                        "Schedule slot was not found"));
        if (slot.getStatus() != ScheduleSlotStatus.BOOKED || !slot.isActive()) {
            throw invalidTransition("The appointment slot cannot be completed");
        }

        Instant now = Instant.now(clock);
        entry.complete(now);
        appointment.complete(now);
        slot.complete();
        queueEntryRepository.saveAndFlush(entry);
        appointmentRepository.saveAndFlush(appointment);
        scheduleSlotRepository.saveAndFlush(slot);
        return toResponse(entry, 0);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QueueEntryResponse skip(Long actorUserId, Long entryId, String rawReason) {
        User actor = findUser(actorUserId);
        String reason = requireReason(rawReason);
        LockedQueue lockedQueue = lockForStaff(actor, entryId);
        QueueEntry entry = lockedQueue.entry();
        requireStatus(entry, QueueStatus.WAITING, "skip");

        entry.skip();
        queueEntryRepository.saveAndFlush(entry);
        queueAuditRepository.saveAndFlush(new QueueAudit(
                entry,
                actor,
                "SKIP",
                QueueStatus.WAITING.name(),
                QueueStatus.SKIPPED.name(),
                reason));
        return toResponse(entry, 0);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QueueEntryResponse returnToWaiting(Long actorUserId, Long entryId) {
        User actor = findUser(actorUserId);
        LockedQueue lockedQueue = lockForStaff(actor, entryId);
        QueueEntry entry = lockedQueue.entry();
        requireStatus(entry, QueueStatus.SKIPPED, "return to waiting");

        entry.returnToWaiting();
        queueEntryRepository.saveAndFlush(entry);
        queueAuditRepository.saveAndFlush(new QueueAudit(
                entry,
                actor,
                "RETURN_TO_WAITING",
                QueueStatus.SKIPPED.name(),
                QueueStatus.WAITING.name(),
                "Queue entry returned to waiting"));
        return toResponse(entry, positionOf(entry));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QueueEntryResponse cancel(Long actorUserId, Long entryId, String rawReason) {
        User actor = findUser(actorUserId);
        String reason = requireReason(rawReason);
        LockedQueue lockedQueue = lockForStaff(actor, entryId);
        QueueEntry entry = lockedQueue.entry();
        if (entry.getStatus() != QueueStatus.WAITING
                && entry.getStatus() != QueueStatus.CALLED) {
            throw invalidTransition("Only a waiting or called queue entry can be cancelled");
        }

        QueueStatus previousStatus = entry.getStatus();
        entry.cancel();
        queueEntryRepository.saveAndFlush(entry);
        queueAuditRepository.saveAndFlush(new QueueAudit(
                entry,
                actor,
                "CANCEL",
                previousStatus.name(),
                QueueStatus.CANCELLED.name(),
                reason));
        return toResponse(entry, 0);
    }

    private List<QueueEntryResponse> listPatientQueue(User actor, LocalDate queueDate) {
        PatientProfile patient = patientProfileRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "PATIENT_PROFILE_NOT_FOUND",
                        "Patient profile was not found"));
        List<QueueEntry> entries = queueEntryRepository.findDetailedQueueForPatient(
                patient.getId(),
                queueDate);
        Map<Long, Integer> positions = waitingPositions(entries, queueDate);
        return entries.stream()
                .map(entry -> toResponse(
                        entry,
                        entry.getStatus() == QueueStatus.WAITING
                                ? positions.getOrDefault(entry.getId(), 0)
                                : 0))
                .toList();
    }

    private List<QueueEntryResponse> listDoctorQueue(
            Long doctorId,
            LocalDate queueDate) {
        List<QueueEntry> entries = queueEntryRepository.findDetailedQueueForDoctor(
                doctorId,
                queueDate);
        Map<Long, Integer> positions = waitingPositions(entries, queueDate);
        return entries.stream()
                .map(entry -> toResponse(
                        entry,
                        entry.getStatus() == QueueStatus.WAITING
                                ? positions.getOrDefault(entry.getId(), 0)
                                : 0))
                .toList();
    }

    private Map<Long, Integer> waitingPositions(
            List<QueueEntry> patientEntries,
            LocalDate queueDate) {
        if (patientEntries.isEmpty()) {
            return Map.of();
        }
        List<Long> doctorIds = patientEntries.stream()
                .map(entry -> entry.getDoctor().getId())
                .distinct()
                .toList();
        List<QueueEntry> waitingEntries = queueEntryRepository.findCanonicalWaitingQueuesForDoctors(
                doctorIds,
                queueDate,
                QueueStatus.WAITING);
        Map<Long, Integer> nextPositionByDoctor = new HashMap<>();
        Map<Long, Integer> positions = new HashMap<>();
        for (QueueEntry entry : waitingEntries) {
            Long doctorId = entry.getDoctor().getId();
            int position = nextPositionByDoctor.getOrDefault(doctorId, 0);
            positions.put(entry.getId(), position);
            nextPositionByDoctor.put(doctorId, position + 1);
        }
        return positions;
    }

    private LockedQueue lockForStaff(User actor, Long entryId) {
        if (actor.getRole() != UserRole.DOCTOR && actor.getRole() != UserRole.ADMIN) {
            throw forbidden();
        }
        QueueEntryRepository.QueueEntryScope scope = queueEntryRepository.findScopeById(entryId)
                .orElseThrow(() -> queueEntryNotFound());
        if (actor.getRole() == UserRole.DOCTOR) {
            DoctorProfile actorDoctor = findDoctorForUser(actor);
            if (!actorDoctor.getId().equals(scope.getDoctorId())) {
                throw forbidden();
            }
        }

        // Clinical lock order: doctor -> appointment -> queue -> slot.
        DoctorProfile doctor = doctorProfileRepository.findByIdForUpdate(scope.getDoctorId())
                .orElseThrow(() -> queueEntryNotFound());
        Appointment appointment = appointmentRepository.findByIdForUpdate(scope.getAppointmentId())
                .orElseThrow(() -> queueEntryNotFound());
        QueueEntry entry = queueEntryRepository.findByIdForUpdate(entryId)
                .orElseThrow(() -> queueEntryNotFound());
        if (!entry.getDoctor().getId().equals(doctor.getId())
                || !entry.getAppointment().getId().equals(appointment.getId())) {
            throw queueEntryNotFound();
        }
        return new LockedQueue(doctor, appointment, entry);
    }

    private void verifyReadAccess(User actor, QueueEntry entry) {
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (actor.getRole() == UserRole.PATIENT
                && entry.getAppointment().getPatient().getUser().getId().equals(actor.getId())) {
            return;
        }
        if (actor.getRole() == UserRole.DOCTOR) {
            DoctorProfile actorDoctor = findDoctorForUser(actor);
            if (actorDoctor.getId().equals(entry.getDoctor().getId())) {
                return;
            }
        }
        throw forbidden();
    }

    private int positionOf(QueueEntry entry) {
        if (entry.getStatus() != QueueStatus.WAITING) {
            return 0;
        }
        long position = queueEntryRepository.countWaitingEntriesAhead(
                entry.getDoctor().getId(),
                entry.getQueueDate(),
                QueueStatus.WAITING,
                entry.getEmergencyTier(),
                entry.getArrivedAt(),
                entry.getId());
        return Math.toIntExact(position);
    }

    private QueueEntryResponse toResponse(QueueEntry entry, int position) {
        Appointment appointment = entry.getAppointment();
        return new QueueEntryResponse(
                entry.getId(),
                appointment.getId(),
                appointment.getPatient().getId(),
                entry.getDoctor().getId(),
                entry.getQueueDate(),
                entry.getTokenNumber(),
                entry.getTokenNumber(),
                entry.getStatus(),
                entry.getEmergencyTier(),
                position,
                entry.getArrivedAt(),
                entry.getCalledAt(),
                entry.getServedAt(),
                entry.getCompletedAt());
    }

    private LocalDate resolveQueueDate(LocalDate requestedDate) {
        if (requestedDate != null) {
            return requestedDate;
        }
        return Instant.now(clock).atZone(hospitalZoneId).toLocalDate();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "User was not found"));
    }

    private DoctorProfile findDoctorForUser(User actor) {
        return doctorProfileRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "DOCTOR_PROFILE_NOT_FOUND",
                        "Doctor profile was not found"));
    }

    private static void requireStatus(
            QueueEntry entry,
            QueueStatus requiredStatus,
            String operation) {
        if (entry.getStatus() != requiredStatus) {
            throw invalidTransition(
                    "Cannot " + operation + " a queue entry in status " + entry.getStatus());
        }
    }

    private static EmergencyTier requireTier(EmergencyTier tier) {
        if (tier == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Request validation failed");
        }
        return tier;
    }

    private static String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "REASON_REQUIRED",
                    "A nonblank reason is required");
        }
        return reason.trim();
    }

    private static ApiException queueEntryNotFound() {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "QUEUE_ENTRY_NOT_FOUND",
                "Queue entry was not found");
    }

    private static ApiException invalidTransition(String message) {
        return new ApiException(
                HttpStatus.CONFLICT,
                "INVALID_QUEUE_TRANSITION",
                message);
    }

    private static ApiException forbidden() {
        return new ApiException(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "Access is denied");
    }

    private record LockedQueue(
            DoctorProfile doctor,
            Appointment appointment,
            QueueEntry entry) {
    }
}
