package com.smarthospital.queue;

public record QueueCheckInResponse(
        Long queueEntryId,
        int token,
        int tokenNumber,
        QueueStatus queueStatus,
        EmergencyTier triageLevel,
        int position) {
}
