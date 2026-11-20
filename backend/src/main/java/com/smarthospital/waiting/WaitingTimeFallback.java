package com.smarthospital.waiting;

import org.springframework.stereotype.Component;

@Component
public class WaitingTimeFallback {

    private static final double EMERGENCY_MINUTES = 5.0;
    private static final double FALLBACK_CONFIDENCE = 0.50;

    public WaitingTimePrediction predict(WaitingTimeFeatures features) {
        int remainingPeople = Math.max(0, features.peopleWaiting());
        int activeEmergencyCount = Math.max(0, features.activeEmergencyCount());
        double calculatedWait = remainingPeople * features.averageConsultationMinutes()
                + activeEmergencyCount * EMERGENCY_MINUTES;
        int estimatedWaitMinutes = Math.max(0, (int) Math.round(calculatedWait));

        return new WaitingTimePrediction(
                estimatedWaitMinutes,
                PredictionSource.FALLBACK,
                FALLBACK_CONFIDENCE,
                null,
                null,
                null);
    }
}
