package com.smarthospital.waiting;

import java.time.Instant;

public record WaitingTimeResponse(
        int estimatedWaitMinutes,
        PredictionSource source,
        double confidence,
        String modelVersion,
        Integer featureSchemaVersion,
        String trainingDataProvenance,
        int queuePosition,
        Instant calculatedAt) {
}
