package com.smarthospital.waiting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class WaitingTimeControllerTest {

    @Test
    void derivesUserIdentityFromAuthenticatedJwtWithoutAcceptingDoctorId() {
        WaitingTimeService service = mock(WaitingTimeService.class);
        WaitingTimeController controller = new WaitingTimeController(service);
        Jwt jwt = Jwt.withTokenValue("access-token")
                .header("alg", "none")
                .claim("sub", "44")
                .build();
        WaitingTimeResponse expected = new WaitingTimeResponse(
                25,
                PredictionSource.ML,
                0.90,
                "waiting-time-rf-v1",
                1,
                "synthetic-demo-data",
                2,
                Instant.parse("2030-01-15T10:00:00Z"));
        when(service.getWaitingTime(101L, 44L)).thenReturn(expected);

        WaitingTimeResponse response = controller.getWaitingTime(101L, jwt);

        assertThat(response).isSameAs(expected);
        verify(service).getWaitingTime(101L, 44L);
    }
}
