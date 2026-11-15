package com.smarthospital.waiting;

import com.smarthospital.common.ApiException;
import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.doctor.DoctorProfileRepository;
import com.smarthospital.patient.PatientProfile;
import com.smarthospital.patient.PatientProfileRepository;
import com.smarthospital.queue.EmergencyTier;
import com.smarthospital.queue.QueueEntry;
import com.smarthospital.queue.QueueEntryRepository;
import com.smarthospital.queue.QueueStatus;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class WaitingTimeService {

    private static final Set<QueueStatus> ACTIVE_QUEUE_STATUSES = Set.of(
            QueueStatus.WAITING,
            QueueStatus.CALLED);

    private final QueueEntryRepository queueEntryRepository;
    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final WaitingTimeClient waitingTimeClient;
    private final WaitingTimeFallback waitingTimeFallback;
    private final Clock clock;
    private final ZoneId hospitalZoneId;

    public WaitingTimeService(
            QueueEntryRepository queueEntryRepository,
            UserRepository userRepository,
            PatientProfileRepository patientProfileRepository,
            DoctorProfileRepository doctorProfileRepository,
            WaitingTimeClient waitingTimeClient,
            WaitingTimeFallback waitingTimeFallback,
            Clock clock,
            ZoneId hospitalZoneId) {
        this.queueEntryRepository = queueEntryRepository;
        this.userRepository = userRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.waitingTimeClient = waitingTimeClient;
        this.waitingTimeFallback = waitingTimeFallback;
        this.clock = clock;
        this.hospitalZoneId = hospitalZoneId;
    }

    public WaitingTimeResponse getWaitingTime(Long entryId, Long authenticatedUserId) {
        QueueEntry entry = queueEntryRepository.findDetailedById(entryId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "QUEUE_ENTRY_NOT_FOUND",
                        "Queue entry was not found"));
        User authenticatedUser = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "User was not found"));

        authorize(entry, authenticatedUser);

        QueueSnapshot queue = queueSnapshot(entry);
        WaitingTimeFeatures features = features(entry, queue);
        WaitingTimePrediction prediction = waitingTimeClient.predict(features)
                .orElseGet(() -> waitingTimeFallback.predict(features));

        return new WaitingTimeResponse(
                prediction.estimatedWaitMinutes(),
                prediction.source(),
                prediction.confidence(),
                prediction.modelVersion(),
                prediction.featureSchemaVersion(),
                prediction.trainingDataProvenance(),
                queue.queuePosition(),
                clock.instant());
    }

    private void authorize(QueueEntry entry, User authenticatedUser) {
        switch (authenticatedUser.getRole()) {
            case PATIENT -> {
                PatientProfile profile = patientProfileRepository
                        .findByUserId(authenticatedUser.getId())
                        .orElseThrow(WaitingTimeService::forbidden);
                Long entryPatientId = entry.getAppointment().getPatient().getId();
                if (!Objects.equals(profile.getId(), entryPatientId)) {
                    throw forbidden();
                }
            }
            case DOCTOR -> {
                DoctorProfile profile = doctorProfileRepository
                        .findByUserId(authenticatedUser.getId())
                        .orElseThrow(WaitingTimeService::forbidden);
                if (!Objects.equals(profile.getId(), entry.getDoctor().getId())) {
                    throw forbidden();
                }
            }
            case ADMIN -> {
            }
            default -> throw forbidden();
        }
    }

    private QueueSnapshot queueSnapshot(QueueEntry entry) {
        List<QueueEntry> activeQueue = queueEntryRepository
                .findCanonicalQueueForDoctorAndStatuses(
                        entry.getDoctor().getId(),
                        entry.getQueueDate(),
                        ACTIVE_QUEUE_STATUSES);

        int queuePosition = entry.getTokenNumber();
        for (int index = 0; index < activeQueue.size(); index++) {
            if (Objects.equals(activeQueue.get(index).getId(), entry.getId())) {
                queuePosition = index + 1;
                break;
            }
        }

        int activeEmergencyCount = (int) activeQueue.stream()
                .filter(queueEntry -> queueEntry.getEmergencyTier() == EmergencyTier.HIGH
                        || queueEntry.getEmergencyTier() == EmergencyTier.CRITICAL)
                .count();
        return new QueueSnapshot(queuePosition, activeQueue.size(), activeEmergencyCount);
    }

    private WaitingTimeFeatures features(QueueEntry entry, QueueSnapshot queue) {
        Instant now = clock.instant();
        ZonedDateTime localNow = ZonedDateTime.ofInstant(now, hospitalZoneId);
        Instant scheduledStart = entry.getAppointment().getSlot().getStartsAt();
        long minutesFromScheduledStart = Math.max(
                0,
                Duration.between(scheduledStart, now).toMinutes());

        return new WaitingTimeFeatures(
                queue.queuePosition(),
                queue.peopleWaiting(),
                entry.getDoctor().getAverageConsultationMinutes(),
                queue.activeEmergencyCount(),
                entry.getEmergencyTier().name(),
                localNow.getHour(),
                localNow.getDayOfWeek().getValue(),
                Math.toIntExact(minutesFromScheduledStart));
    }

    private static ApiException forbidden() {
        return new ApiException(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "Access is denied");
    }

    private record QueueSnapshot(
            int queuePosition,
            int peopleWaiting,
            int activeEmergencyCount) {
    }
}
