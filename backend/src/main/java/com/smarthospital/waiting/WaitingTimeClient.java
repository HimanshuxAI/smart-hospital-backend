package com.smarthospital.waiting;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class WaitingTimeClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitingTimeClient.class);

    private static final ObjectMapper STRICT_JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .build();

    private static final Set<String> REQUIRED_RESPONSE_FIELDS = Set.of(
            "estimatedWaitMinutes",
            "confidence",
            "modelVersion",
            "featureSchemaVersion",
            "trainingDataProvenance");

    private final RestClient restClient;

    public WaitingTimeClient(@Qualifier("mlRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Optional<WaitingTimePrediction> predict(WaitingTimeFeatures features) {
        try {
            String responseBody = restClient.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(features)
                    .retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful(),
                            (request, response) -> {
                                throw new HttpClientErrorException(response.getStatusCode());
                            })
                    .body(String.class);
            Optional<MlPredictionPayload> payload = parseStrict(responseBody);
            if (payload.isEmpty()) {
                LOGGER.warn("ML service returned an invalid waiting-time prediction");
                return Optional.empty();
            }
            MlPredictionPayload parsed = payload.orElseThrow();
            return Optional.of(new WaitingTimePrediction(
                    parsed.estimatedWaitMinutes(),
                    PredictionSource.ML,
                    parsed.confidence(),
                    parsed.modelVersion(),
                    parsed.featureSchemaVersion(),
                    parsed.trainingDataProvenance()));
        } catch (RestClientException | IllegalArgumentException exception) {
            LOGGER.warn("ML waiting-time prediction unavailable: {}",
                    exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static Optional<MlPredictionPayload> parseStrict(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return Optional.empty();
        }

        try {
            JsonNode root = STRICT_JSON.readTree(responseBody);
            if (root == null || !root.isObject()) {
                return Optional.empty();
            }

            Set<String> responseFields = new HashSet<>();
            root.fieldNames().forEachRemaining(responseFields::add);
            if (!responseFields.equals(REQUIRED_RESPONSE_FIELDS)) {
                return Optional.empty();
            }

            JsonNode estimatedWaitMinutes = root.get("estimatedWaitMinutes");
            if (!estimatedWaitMinutes.isIntegralNumber()
                    || !estimatedWaitMinutes.canConvertToInt()
                    || estimatedWaitMinutes.intValue() < 0) {
                return Optional.empty();
            }

            JsonNode confidence = root.get("confidence");
            if (!confidence.isNumber()
                    || !Double.isFinite(confidence.doubleValue())
                    || confidence.doubleValue() < 0.0
                    || confidence.doubleValue() > 1.0) {
                return Optional.empty();
            }

            JsonNode modelVersion = root.get("modelVersion");
            if (!modelVersion.isTextual() || modelVersion.textValue().isBlank()) {
                return Optional.empty();
            }

            JsonNode featureSchemaVersion = root.get("featureSchemaVersion");
            if (!featureSchemaVersion.isIntegralNumber()
                    || !featureSchemaVersion.canConvertToInt()
                    || featureSchemaVersion.intValue() < 1) {
                return Optional.empty();
            }

            JsonNode trainingDataProvenance = root.get("trainingDataProvenance");
            if (!trainingDataProvenance.isTextual()
                    || trainingDataProvenance.textValue().isBlank()) {
                return Optional.empty();
            }

            return Optional.of(new MlPredictionPayload(
                    estimatedWaitMinutes.intValue(),
                    confidence.doubleValue(),
                    modelVersion.textValue(),
                    featureSchemaVersion.intValue(),
                    trainingDataProvenance.textValue()));
        } catch (JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    private record MlPredictionPayload(
            int estimatedWaitMinutes,
            double confidence,
            String modelVersion,
            int featureSchemaVersion,
            String trainingDataProvenance) {
    }
}
