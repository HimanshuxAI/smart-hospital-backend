package com.smarthospital.waiting;

public record WaitingTimePrediction(
        int estimatedWaitMinutes,
        PredictionSource source,
        double confidence,
        String modelVersion,
        Integer featureSchemaVersion,
        String trainingDataProvenance) {
}
