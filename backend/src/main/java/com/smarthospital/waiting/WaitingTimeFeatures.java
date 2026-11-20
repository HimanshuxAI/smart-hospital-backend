package com.smarthospital.waiting;

public record WaitingTimeFeatures(
        int queuePosition,
        int peopleWaiting,
        double averageConsultationMinutes,
        int activeEmergencyCount,
        String triageTier,
        int hourOfDay,
        int dayOfWeek,
        int minutesFromScheduledStart) {
}
