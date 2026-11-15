package com.smarthospital.waiting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

class WaitingTimeClientTest {

    private static final WaitingTimeFeatures FEATURES = new WaitingTimeFeatures(
            3,
            5,
            15.0,
            1,
            "MEDIUM",
            10,
            2,
            20);

    private MockRestServiceServer server;
    private WaitingTimeClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new WaitingTimeClient(
                builder.baseUrl("http://ml.test").build());
    }

    @Test
    void successfulPredictionIsReturnedWithMlMetadata() {
        server.expect(requestTo("http://ml.test/predict"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        """
                        {
                          "estimatedWaitMinutes": 42,
                          "confidence": 0.91,
                          "modelVersion": "waiting-time-rf-v1",
                          "featureSchemaVersion": 1,
                          "trainingDataProvenance": "synthetic-demo-data"
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        var prediction = client.predict(FEATURES);

        assertThat(prediction).isPresent();
        assertThat(prediction.orElseThrow().estimatedWaitMinutes()).isEqualTo(42);
        assertThat(prediction.orElseThrow().source()).isEqualTo(PredictionSource.ML);
        assertThat(prediction.orElseThrow().confidence()).isEqualTo(0.91);
        assertThat(prediction.orElseThrow().modelVersion()).isEqualTo("waiting-time-rf-v1");
        assertThat(prediction.orElseThrow().featureSchemaVersion()).isEqualTo(1);
        assertThat(prediction.orElseThrow().trainingDataProvenance())
                .isEqualTo("synthetic-demo-data");
        server.verify();
    }

    @Test
    void nonSuccessStatusReturnsNoPrediction() {
        server.expect(requestTo("http://ml.test/predict"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThat(client.predict(FEATURES)).isEmpty();
        server.verify();
    }

    @Test
    void timeoutReturnsNoPrediction() {
        server.expect(requestTo("http://ml.test/predict"))
                .andRespond(request -> {
                    throw new ResourceAccessException("Read timed out");
                });

        assertThat(client.predict(FEATURES)).isEmpty();
        server.verify();
    }

    @Test
    void invalidResponseReturnsNoPrediction() {
        server.expect(requestTo("http://ml.test/predict"))
                .andRespond(withSuccess(
                        """
                        {
                          "estimatedWaitMinutes": -4,
                          "confidence": 2.0,
                          "modelVersion": "",
                          "featureSchemaVersion": 0,
                          "trainingDataProvenance": ""
                        }
                        """,
                        MediaType.APPLICATION_JSON));

        assertThat(client.predict(FEATURES)).isEmpty();
        server.verify();
    }

    @Test
    void malformedJsonReturnsNoPrediction() {
        server.expect(requestTo("http://ml.test/predict"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        assertThat(client.predict(FEATURES)).isEmpty();
        server.verify();
    }
}
