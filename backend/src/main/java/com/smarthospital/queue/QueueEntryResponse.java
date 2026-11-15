package com.smarthospital.queue;

import java.time.Instant;
import java.time.LocalDate;

public record QueueEntryResponse(
        Long queueEntryId,
        Long appointmentId,
        Long patientId,
        Long doctorId,
        LocalDate queueDate,
        int token,
        int tokenNumber,
        QueueStatus queueStatus,
        EmergencyTier triageLevel,
        int position,
        Instant arrivedAt,
        Instant calledAt,
        Instant servedAt,
        Instant completedAt) {
}
