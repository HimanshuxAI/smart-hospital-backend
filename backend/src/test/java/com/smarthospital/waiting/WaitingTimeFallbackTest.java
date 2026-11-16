package com.smarthospital.waiting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WaitingTimeFallbackTest {

    private final WaitingTimeFallback fallback = new WaitingTimeFallback();

    @Test
    void calculatesNearestMinuteUsingQueueAndEmergencyLoad() {
        WaitingTimeFeatures features = new WaitingTimeFeatures(
                4,
                2,
                10.4,
                1,
                "NORMAL",
                10,
                2,
                15);

        WaitingTimePrediction prediction = fallback.predict(features);

        assertThat(prediction.estimatedWaitMinutes()).isEqualTo(26);
        assertThat(prediction.source()).isEqualTo(PredictionSource.FALLBACK);
        assertThat(prediction.confidence()).isEqualTo(0.50);
        assertThat(prediction.modelVersion()).isNull();
        assertThat(prediction.featureSchemaVersion()).isNull();
        assertThat(prediction.trainingDataProvenance()).isNull();
    }

    @Test
    void clampsNegativeCalculatedWaitToZero() {
        WaitingTimeFeatures features = new WaitingTimeFeatures(
                1,
                -2,
                15.0,
                0,
                "NORMAL",
                10,
                2,
                0);

        WaitingTimePrediction prediction = fallback.predict(features);

        assertThat(prediction.estimatedWaitMinutes()).isZero();
        assertThat(prediction.source()).isEqualTo(PredictionSource.FALLBACK);
        assertThat(prediction.confidence()).isEqualTo(0.50);
    }
}
