package com.smarthospital.waiting;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class WaitingTimeClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitingTimeClient.class);

    private final RestClient restClient;

    public WaitingTimeClient(@Qualifier("mlRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Optional<WaitingTimePrediction> predict(WaitingTimeFeatures features) {
        try {
            MlPredictionPayload payload = restClient.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(features)
                    .retrieve()
                    .body(MlPredictionPayload.class);
            if (!isValid(payload)) {
                LOGGER.warn("ML service returned an invalid waiting-time prediction");
                return Optional.empty();
            }
            return Optional.of(new WaitingTimePrediction(
                    payload.estimatedWaitMinutes(),
                    PredictionSource.ML,
                    payload.confidence(),
                    payload.modelVersion(),
                    payload.featureSchemaVersion(),
                    payload.trainingDataProvenance()));
        } catch (RestClientException | IllegalArgumentException exception) {
            LOGGER.warn("ML waiting-time prediction unavailable: {}",
                    exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static boolean isValid(MlPredictionPayload payload) {
        if (payload == null
                || payload.estimatedWaitMinutes() == null
                || payload.estimatedWaitMinutes() < 0
                || payload.confidence() == null
                || !Double.isFinite(payload.confidence())
                || payload.confidence() < 0.0
                || payload.confidence() > 1.0
                || payload.modelVersion() == null
                || payload.modelVersion().isBlank()
                || payload.featureSchemaVersion() == null
                || payload.featureSchemaVersion() < 1
                || payload.trainingDataProvenance() == null
                || payload.trainingDataProvenance().isBlank()) {
            return false;
        }
        return true;
    }

    private record MlPredictionPayload(
            Integer estimatedWaitMinutes,
            Double confidence,
            String modelVersion,
            Integer featureSchemaVersion,
            String trainingDataProvenance) {
    }
}
